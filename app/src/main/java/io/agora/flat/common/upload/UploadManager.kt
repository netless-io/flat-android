package io.agora.flat.common.upload

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object UploadManager {
    private const val DEFAULT_THREAD_POOL_SIZE = 4

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE) { r ->
        Thread(r, "flat-upload-manager-thread")
    }

    const val UPLOAD_TAG_DEFAULT = "_upload_tag_"

    private lateinit var application: Application
    private val taskMap = mutableMapOf<String, UploadTask>()

    fun init(application: Application) {
        UploadManager.application = application
    }

    fun upload(request: UploadRequest) {
        val task = UploadTask(application, client, request, localEventListener)
        taskMap[request.uuid] = task
        executorService.submit(task)

        uploadFiles.value = uploadFiles.value.toMutableList().apply {
            add(
                0,
                UploadFile(fileUUID = request.uuid, filename = request.filename, size = request.size, tag = request.tag)
            )
        }
    }

    fun cancel(fileUUID: String) {
        taskMap[fileUUID]?.also {
            it.cancel()
        }

        uploadFiles.value = uploadFiles.value.toMutableList().filter { it.fileUUID != fileUUID }
    }

    fun retry(fileUUID: String) {
        taskMap[fileUUID]?.also {
            executorService.submit(it)
        }
    }

    private var uploadFiles = MutableStateFlow(listOf<UploadFile>())
    private var uploadSuccess = MutableStateFlow<UploadRequest?>(null)

    fun observeUploadFiles(tag: String = UPLOAD_TAG_DEFAULT): Flow<List<UploadFile>> {
        return uploadFiles.map { it.filter { f -> f.tag == tag } }
    }

    fun observeSuccess(tag: String = UPLOAD_TAG_DEFAULT): Flow<UploadRequest> {
        return uploadSuccess.filterNotNull().filter { it.tag == tag }
    }

    private val localEventListener = UploadEventListener { event ->
        updateUploadFiles(event)
        if (event is UploadEvent.State && event.uploadState == UploadState.Success) {
            uploadSuccess.value = taskMap[event.fileUUID]?.request
            taskMap.remove(event.fileUUID)
        }
    }

    private fun updateUploadFiles(event: UploadEvent) {
        val index = uploadFiles.value.indexOfFirst { it.fileUUID == event.fileUUID }
        if (index >= 0) {
            val oldFile = uploadFiles.value[index]
            val newFile = when (event) {
                is UploadEvent.Progress -> oldFile.copy(progress = event.currentSize.toFloat() / event.totalSize)
                is UploadEvent.State -> oldFile.copy(uploadState = event.uploadState)
            }
            uploadFiles.value = uploadFiles.value.toMutableList().apply {
                set(index, newFile)
            }
        }
    }
}

data class UploadRequest constructor(
    // remote info
    val uuid: String,
    val policy: String,
    val policyURL: String,
    val filepath: String,
    val signature: String,

    val ossKey: String,

    // local info
    val filename: String,
    val size: Long,
    val mediaType: String,
    val uri: Uri,

    val tag: String = UploadManager.UPLOAD_TAG_DEFAULT,
)

private class UploadTask(
    val context: Context,
    val client: OkHttpClient,
    val request: UploadRequest,
    val eventListener: UploadEventListener,
    val contentResolver: ContentResolver = context.contentResolver,
) : Callable<Unit> {
    private var callRef: Call? = null

    @Volatile
    private var canceled = false

    fun cancel() {
        if (canceled) return
        callRef?.cancel()
        canceled = true
    }

    override fun call() {
        val filename = request.filename
        val mediaType = request.mediaType
        val size = request.size
        val inputStream = contentResolver.openInputStream(request.uri)

        val fileBody = ProgressRequestBody(
            mediaType.toMediaType(),
            inputStream,
            size,
            LocalProgressListener(request.uuid, eventListener)
        )

        val encodeFileName = Uri.encode(filename)
        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("key", request.filepath)
            addFormDataPart("name", filename)
            addFormDataPart("policy", request.policy)
            addFormDataPart("OSSAccessKeyId", request.ossKey)
            addFormDataPart("success_action_status", "200")
            addFormDataPart("callback", "")
            addFormDataPart("signature", request.signature)
            addFormDataPart(
                "Content-Disposition",
                "attachment; filename=\"${encodeFileName}\"; filename*=UTF-8''${encodeFileName}"
            )
            addFormDataPart("file", encodeFileName, fileBody)
        }.build()

        val httpRequest = Request.Builder()
            .url(request.policyURL)
            .post(requestBody)
            .build()

        eventListener.onEvent(UploadEvent.State(request.uuid, UploadState.Uploading))
        try {
            val call = client.newCall(httpRequest)
            callRef = call
            val response = call.execute()
            if (response.isSuccessful) {
                eventListener.onEvent(UploadEvent.State(request.uuid, UploadState.Success))
            } else {
                eventListener.onEvent(UploadEvent.State(request.uuid, UploadState.Failure))
            }
        } catch (e: Exception) {
            eventListener.onEvent(UploadEvent.State(request.uuid, UploadState.Failure))
        }
    }

    class ProgressRequestBody(
        private val contentType: MediaType,
        private val inputStream: InputStream?,
        private val totalSize: Long,
        private val onProgressListener: OnProgressListener,
    ) : RequestBody() {

        companion object {
            const val SEGMENT_SIZE = 2048L
        }

        override fun contentType() = contentType

        override fun contentLength(): Long = totalSize

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            inputStream?.use { input ->
                val source = input.source()
                var currentSize: Long = 0
                var count: Long
                while ((source.read(sink.buffer, SEGMENT_SIZE).also { count = it }) != -1L) {
                    currentSize += count
                    sink.flush()
                    onProgressListener.onProgress(currentSize, totalSize)
                }
                onProgressListener.onProgress(totalSize, totalSize)
            }
        }
    }

    class LocalProgressListener(val fileUUID: String, val eventListener: UploadEventListener) : OnProgressListener {
        private var nextUpdate = System.currentTimeMillis()
        override fun onProgress(currentSize: Long, totalSize: Long) {
            if (System.currentTimeMillis() > nextUpdate || currentSize == totalSize) {
                nextUpdate += 1000
                eventListener.onEvent(UploadEvent.Progress(fileUUID, currentSize, totalSize))
            }
        }
    }
}

internal interface OnProgressListener {
    fun onProgress(currentSize: Long, totalSize: Long)
}

fun interface UploadEventListener {
    fun onEvent(event: UploadEvent)
}

data class UploadFile(
    val fileUUID: String,
    val filename: String,
    val size: Long = 0,
    val uploadState: UploadState = UploadState.Init,
    val progress: Float = 0.0F,
    val tag: String = UploadManager.UPLOAD_TAG_DEFAULT,
)

enum class UploadState {
    Init,
    Uploading,
    Success,
    Failure,
}

sealed class UploadEvent(open val fileUUID: String) {
    data class Progress(
        override val fileUUID: String,
        val currentSize: Long,
        val totalSize: Long
    ) : UploadEvent(fileUUID)

    data class State(
        override val fileUUID: String,
        val uploadState: UploadState
    ) : UploadEvent(fileUUID)
}