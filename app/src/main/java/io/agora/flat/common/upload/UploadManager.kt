package io.agora.flat.common.upload

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.agora.flat.Constants
import io.agora.flat.util.contentFileInfo
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

    private lateinit var application: Application
    private val taskMap = mutableMapOf<String, UploadTask>()

    fun init(application: Application) {
        UploadManager.application = application
    }

    fun upload(request: UploadRequest, eventListener: UploadEventListener) {
        val task = UploadTask(application, client, request, { event ->
            if (event is UploadStateEvent && event.uploadState == UploadState.Success) {
                taskMap.remove(event.fileUUID)
            }
            eventListener.onEvent(event)
        })
        taskMap[request.fileUUID] = task
        executorService.submit(task)
    }

    fun cancel(fileUUID: String) {
        taskMap[fileUUID]?.also {
            it.cancel()
        }
    }

    fun retry(fileUUID: String) {
        taskMap[fileUUID]?.also {
            executorService.submit(it)
        }
    }
}

data class UploadRequest constructor(
    // remote info
    val fileUUID: String,
    val filePath: String,
    val policy: String,
    val policyURL: String,
    val signature: String,

    // local info
    val uri: Uri,
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
        val info = context.contentFileInfo(request.uri)
            ?: throw RuntimeException("get file info error")

        val (filename, size, mediaType) = info
        val inputStream = contentResolver.openInputStream(request.uri)

        val fileBody = ProgressRequestBody(
            mediaType.toMediaType(),
            inputStream,
            size,
            LocalProgressListener(request.fileUUID, eventListener)
        )

        val encodeFileName = Uri.encode(filename)
        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("key", request.filePath)
            addFormDataPart("name", filename)
            addFormDataPart("policy", request.policy)
            addFormDataPart("OSSAccessKeyId", Constants.OSS_ACCESS_KEY_ID)
            addFormDataPart("success_action_status", "200")
            addFormDataPart("callback", "")
            addFormDataPart("signature", request.signature)
            addFormDataPart("Content-Disposition",
                "attachment; filename=\"${encodeFileName}\"; filename*=UTF-8''${encodeFileName}")
            addFormDataPart("file", encodeFileName, fileBody)
        }.build()

        val httpRequest = Request.Builder()
            .url(request.policyURL)
            .post(requestBody)
            .build()

        eventListener.onEvent(UploadStateEvent(request.fileUUID, UploadState.Uploading))
        try {
            val call = client.newCall(httpRequest)
            callRef = call
            val response = call.execute()
            if (response.isSuccessful) {
                eventListener.onEvent(UploadStateEvent(request.fileUUID, UploadState.Success))
            } else {
                eventListener.onEvent(UploadStateEvent(request.fileUUID, UploadState.Failure))
            }
        } catch (e: Exception) {
            eventListener.onEvent(UploadStateEvent(request.fileUUID, UploadState.Failure))
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
                eventListener.onEvent(UploadProgressEvent(fileUUID, currentSize, totalSize))
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

enum class UploadState {
    Init,
    Uploading,
    Success,
    Failure,
}

sealed class UploadEvent
data class UploadProgressEvent(val fileUUID: String, val currentSize: Long, val totalSize: Long) : UploadEvent()
data class UploadStateEvent(val fileUUID: String, val uploadState: UploadState) : UploadEvent()