package io.agora.flat.ui.activity.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herewhite.sdk.ConverterCallbacks
import com.herewhite.sdk.converter.ConvertType
import com.herewhite.sdk.converter.ConverterV5
import com.herewhite.sdk.domain.ConversionInfo
import com.herewhite.sdk.domain.ConvertException
import com.herewhite.sdk.domain.ConvertedFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.upload.*
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.CloudStorageUploadStartResp
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.fileSuffix
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
) : ViewModel() {
    companion object {
        const val TAG = "CloudStorageViewModel"
    }

    private val files = MutableStateFlow(listOf<CloudStorageUIFile>())
    private val uploadFiles = MutableStateFlow(listOf<UploadFile>())
    private val totalUsage = MutableStateFlow(0L)
    private val pageLoading = ObservableLoadingCounter()
    private val refreshing = ObservableLoadingCounter()

    private val _state = MutableStateFlow(CloudStorageViewState())
    val state: StateFlow<CloudStorageViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                refreshing.observable,
                totalUsage,
                files,
                uploadFiles,
            ) { refreshing, totalUsage, files, uploadFiles ->
                CloudStorageViewState(
                    refreshing = refreshing,
                    totalUsage = totalUsage,
                    files = files,
                    uploadFiles = uploadFiles,
                    errorMessage = null,
                )
            }.collect {
                _state.value = it
            }
        }

        reloadFileList()
    }

    fun reloadFileList() {
        viewModelScope.launch {
            refreshing.addLoader()
            runAtLeast {
                val resp = cloudStorageRepository.getFileList(1)
                if (resp is Success) {
                    totalUsage.value = resp.data.totalUsage
                    files.value = resp.data.files.reversed().map {
                        CloudStorageUIFile(
                            fileUUID = it.fileUUID,
                            filename = it.fileName,
                            fileSize = it.fileSize,
                            fileURL = it.fileURL,
                            convertStep = it.convertStep,
                            taskUUID = it.taskUUID,
                            taskToken = it.taskToken,
                            createAt = it.createAt,
                            checked = false
                        )
                    }
                } else {
                    // do nothing
                }
            }
            refreshing.removeLoader()
        }
    }

    fun checkItem(action: CloudStorageUIAction.CheckItem) {
        viewModelScope.launch {
            val fs = files.value.toMutableList()
            fs[action.index] = fs[action.index].copy(checked = action.checked)
            files.value = fs
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            pageLoading.addLoader()
            val checked = files.value.filter { it.checked }
            val result = cloudStorageRepository.remove(checked.map { it.fileUUID })
            if (result is Success) {
                files.value = files.value.filterNot { it.checked }

                val size = checked.sumOf { it.fileSize }
                totalUsage.value = totalUsage.value - size
            }
            pageLoading.removeLoader()
        }
    }

    fun uploadFile(action: CloudStorageUIAction.UploadFile) {
        viewModelScope.launch {
            var result: CloudStorageUploadStartResp? = null
            var resp = cloudStorageRepository.updateStart(action.filename, action.size)
            when (resp) {
                is Success -> result = resp.data
                is Failure -> {
                    if (resp.error.code == FlatErrorCode.Web_UploadConcurrentLimit) {
                        cloudStorageRepository.cancel()
                        // retry
                        resp = cloudStorageRepository.updateStart(action.filename, action.size)
                        if (resp is Success) {
                            result = resp.data
                        }
                    }
                }
            }

            if (result == null) {
                return@launch
            }

            val request = UploadRequest(
                fileUUID = result.fileUUID,
                filePath = result.filePath,
                policy = result.policy,
                policyURL = result.policyURL,
                signature = result.signature,
                uri = action.uri
            )
            UploadManager.upload(request) {
                updateUploadFiles(it)
                if (it is UploadStateEvent && it.uploadState == UploadState.Success) {
                    handleUploadSuccess(request.fileUUID, action.filename, action.size)
                }
            }

            uploadFiles.value = uploadFiles.value.toMutableList().apply {
                add(0, UploadFile(fileUUID = result.fileUUID, fileName = action.filename))
            }
        }
    }

    private fun updateUploadFiles(event: UploadEvent) = when (event) {
        is UploadProgressEvent -> {
            uploadFiles.value.indexOfFirst { event.fileUUID == it.fileUUID }.let { index ->
                if (index < 0) return
                val changed = uploadFiles.value[index].copy(progress = event.currentSize * 1f / event.totalSize)
                uploadFiles.value = uploadFiles.value.toMutableList().apply { set(index, changed) }
            }
        }
        is UploadStateEvent -> {
            uploadFiles.value.indexOfFirst { event.fileUUID == it.fileUUID }.let { index ->
                if (index < 0) return
                val changed = uploadFiles.value[index].copy(uploadState = event.uploadState)
                uploadFiles.value = uploadFiles.value.toMutableList().apply { set(index, changed) }
            }
        }
    }

    private fun handleUploadSuccess(fileUUID: String, filename: String, size: Long) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.updateFinish(fileUUID)
            if (resp is Success) {
                delayRemoveSuccess(fileUUID)
                when (filename.fileSuffix()) {
                    "ppt", "pptx" -> startConvert(fileUUID, true)
                    "pdf" -> startConvert(fileUUID, false)
                    else -> {; }
                }

                files.value = files.value.toMutableList().apply {
                    add(0, CloudStorageUIFile(
                        fileUUID = fileUUID,
                        filename = filename,
                        fileSize = size,
                        fileURL = filename,
                        createAt = System.currentTimeMillis()
                    ))
                }
                totalUsage.value = totalUsage.value + size
            }
        }
    }

    private fun startConvert(fileUUID: String, dynamic: Boolean) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.convertStart(fileUUID)
            if (resp is Success) {
                val converterV5 = ConverterV5.Builder().apply {
                    setResource("")
                    setType(if (dynamic) ConvertType.Dynamic else ConvertType.Static)
                    setTaskToken(resp.data.taskToken)
                    setTaskUuid(resp.data.taskUUID)
                    setCallback(object : ConverterCallbacks {
                        override fun onProgress(progress: Double, convertInfo: ConversionInfo) {
                        }

                        override fun onFinish(ppt: ConvertedFiles?, convertInfo: ConversionInfo) {
                            finishConvert(fileUUID, true)
                        }

                        override fun onFailure(e: ConvertException) {
                            finishConvert(fileUUID, false)
                        }
                    })
                }.build()
                converterV5.startConvertTask()

                updateConvertStep(fileUUID, FileConvertStep.Converting)
            }
        }
    }

    private fun finishConvert(fileUUID: String, success: Boolean) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.convertFinish(fileUUID)
            if (resp is Success) {
                updateConvertStep(fileUUID, if (success) FileConvertStep.Done else FileConvertStep.Failed)
            } else {
                updateConvertStep(fileUUID, FileConvertStep.Failed)
            }
        }
    }

    private fun updateConvertStep(fileUUID: String, convertStep: FileConvertStep) {
        files.value.indexOfFirst { fileUUID == it.fileUUID }.let { index ->
            if (index < 0) return
            val changed = files.value[index].copy(convertStep = convertStep)
            files.value = files.value.toMutableList().apply { set(index, changed) }
        }
    }

    private fun delayRemoveSuccess(fileUUID: String) {
        viewModelScope.launch {
            delay(3000)
            uploadFiles.value = uploadFiles.value.toMutableList().filter { it.fileUUID != fileUUID }
        }
    }

    fun retryUpload(fileUUID: String) {
        viewModelScope.launch {
            UploadManager.retry(fileUUID)
        }
    }

    fun deleteUpload(fileUUID: String) {
        viewModelScope.launch {
            UploadManager.cancel(fileUUID)
            uploadFiles.value = uploadFiles.value.toMutableList().filter { it.fileUUID != fileUUID }
        }
    }
}

data class CloudStorageUIFile(
    val fileUUID: String,
    val filename: String,
    val fileSize: Long = 0,
    val fileURL: String = "",
    val convertStep: FileConvertStep = FileConvertStep.None,
    val taskUUID: String? = null,
    val taskToken: String? = null,
    val createAt: Long = 0,
    val checked: Boolean = false,
)

data class UploadFile(
    val fileUUID: String,
    val fileName: String,
    val uploadState: UploadState = UploadState.Init,
    val progress: Float = 0.0F,
)

data class CloudStorageViewState(
    val refreshing: Boolean = false,
    val totalUsage: Long = 0,
    val files: List<CloudStorageUIFile> = emptyList(),
    val uploadFiles: List<UploadFile> = emptyList(),
    val pageLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class CloudStorageUIAction {
    object Delete : CloudStorageUIAction()
    object Reload : CloudStorageUIAction()
    data class CheckItem(val index: Int, val checked: Boolean) : CloudStorageUIAction()
    data class ClickItem(val index: Int) : CloudStorageUIAction()

    data class UploadFile(val filename: String, val size: Long, val uri: Uri) : CloudStorageUIAction()
    data class UploadRetry(val fileUUID: String) : CloudStorageUIAction()
    data class UploadDelete(val fileUUID: String) : CloudStorageUIAction()
}