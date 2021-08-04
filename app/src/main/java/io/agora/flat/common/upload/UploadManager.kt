package io.agora.flat.common.upload

import android.app.Application
import android.content.ContentResolver
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
import java.util.concurrent.TimeUnit

object UploadManager {
    lateinit var application: Application
    lateinit var contentResolver: ContentResolver
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val callMap = mutableMapOf<String, Call>()
    private val taskMap = mutableMapOf<String, UploadTask>()

    fun init(application: Application) {
        UploadManager.application = application
        contentResolver = application.contentResolver
    }

    fun upload(uploadRequest: UploadRequest, uploadEventListener: UploadEventListener) {
        val fileInfo = application.contentFileInfo(uri = uploadRequest.uri) ?: return
        val (fileName, size, mediaType) = fileInfo
        val fileBody = InputStreamRequestBody(
            mediaType.toMediaType(),
            contentResolver.openInputStream(uploadRequest.uri),
            size,
            object : OnProgressListener {
                private var nextUpdate = System.currentTimeMillis()
                override fun onProgress(currentSize: Long, totalSize: Long) {
                    if (System.currentTimeMillis() > nextUpdate || currentSize == totalSize) {
                        nextUpdate += 1000
                        uploadEventListener.onEvent(UploadEvent.UploadProgressEvent(uploadRequest.fileUUID,
                            currentSize,
                            totalSize))
                    }
                }
            }
        )

        val encodeFileName = Uri.encode(fileName)
        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("key", uploadRequest.filePath)
            addFormDataPart("name", fileName)
            addFormDataPart("policy", uploadRequest.policy)
            addFormDataPart("OSSAccessKeyId", Constants.OSS_ACCESS_KEY_ID)
            addFormDataPart("success_action_status", "200")
            addFormDataPart("callback", "")
            addFormDataPart("signature", uploadRequest.signature)
            addFormDataPart("Content-Disposition",
                "attachment; filename=\"${encodeFileName}\"; filename*=UTF-8''${encodeFileName}")
            addFormDataPart("file", encodeFileName, fileBody)
        }.build()

        val request = Request.Builder()
            .url(uploadRequest.policyURL)
            .post(requestBody)
            .build()

        val fileUUID = uploadRequest.fileUUID
        taskMap[fileUUID] = UploadTask(uploadRequest, uploadEventListener)

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    uploadEventListener.onEvent(UploadEvent.UploadStateEvent(uploadRequest.fileUUID,
                        UploadState.Success))
                    taskMap.remove(fileUUID)
                } else {
                    uploadEventListener.onEvent(UploadEvent.UploadStateEvent(uploadRequest.fileUUID,
                        UploadState.Failure))
                }
                callMap.remove(fileUUID)
            }

            override fun onFailure(call: Call, e: IOException) {
                uploadEventListener.onEvent(UploadEvent.UploadStateEvent(uploadRequest.fileUUID, UploadState.Failure))
                callMap.remove(fileUUID)
            }
        })
        uploadEventListener.onEvent(UploadEvent.UploadStateEvent(uploadRequest.fileUUID, UploadState.Uploading))
        callMap[fileUUID] = call
    }

    fun cancel(fileUUID: String) {
        callMap[fileUUID]?.cancel()
        callMap.remove(fileUUID)
    }

    fun retry(fileUUID: String) {
        cancel(fileUUID)
        taskMap[fileUUID]?.run {
            upload(uploadRequest, uploadEventListener)
        }
    }
}

data class UploadRequest constructor(
    val fileUUID: String,
    val filePath: String,
    val policy: String,
    val policyURL: String,
    val signature: String,
    val uri: Uri,
)

private class UploadTask constructor(val uploadRequest: UploadRequest, val uploadEventListener: UploadEventListener) {

}

internal class InputStreamRequestBody(
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
                currentSize += count;
                sink.flush();
                onProgressListener.onProgress(currentSize, totalSize)
            }
            onProgressListener.onProgress(totalSize, totalSize)
        }
    }
}

internal interface OnProgressListener {
    fun onProgress(currentSize: Long, totalSize: Long)
}

interface UploadEventListener {
    fun onEvent(event: UploadEvent)
}

enum class UploadState {
    Init,
    Uploading,
    Canceled,
    Success,
    Failure,
}

sealed class UploadEvent {
    data class UploadProgressEvent(val fileUUID: String, val currentSize: Long, val totalSize: Long) : UploadEvent()

    data class UploadStateEvent(val fileUUID: String, val uploadState: UploadState) : UploadEvent()
}