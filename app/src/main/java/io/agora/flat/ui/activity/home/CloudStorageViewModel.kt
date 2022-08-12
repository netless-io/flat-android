package io.agora.flat.ui.activity.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herewhite.sdk.ConverterCallbacks
import com.herewhite.sdk.converter.ConvertType
import com.herewhite.sdk.converter.ConverterV5
import com.herewhite.sdk.converter.ProjectorQuery
import com.herewhite.sdk.domain.ConversionInfo
import com.herewhite.sdk.domain.ConvertException
import com.herewhite.sdk.domain.ConvertedFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.FlatNetException
import io.agora.flat.common.upload.UploadFile
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.ContentInfo
import io.agora.flat.util.coursewareType
import io.agora.flat.util.isDynamicDoc
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val appKVCenter: AppKVCenter,
) : ViewModel() {
    private val files = MutableStateFlow(listOf<CloudStorageUIFile>())
    private val totalUsage = MutableStateFlow(0L)
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
                UploadManager.uploadFiles,
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

        viewModelScope.launch {
            UploadManager.uploadSuccess.filterNotNull().collect {
                handleUploadSuccess(fileUUID = it.uuid, filename = it.filename, size = it.size)
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
                    files.value = resp.data.files.map {
                        CloudStorageUIFile(file = it, checked = false)
                    }
                    resp.data.files.forEach(::checkConvertState)
                } else {
                    // do nothing
                }
            }
            refreshing.removeLoader()
        }
    }

    private fun checkConvertState(file: CloudStorageFile) {
        when (file.convertStep) {
            FileConvertStep.None -> {
                checkAndStartConvert(file.fileURL, file.fileUUID)
            }
            FileConvertStep.Converting -> {
                if (file.resourceType == ResourceType.WhiteboardConvert) {
                    startConvertQuery(file.fileURL.isDynamicDoc(), file.taskToken, file.taskUUID, file.fileUUID)
                }
                if (file.resourceType == ResourceType.WhiteboardProjector) {
                    startProjectorQuery(
                        taskUUID = file.taskUUID,
                        taskToken = file.taskToken,
                        fileUUID = file.fileUUID,
                    )
                }
            }
            else -> {}
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
            refreshing.addLoader()
            val checked = files.value.filter { it.checked }
            val result = cloudStorageRepository.remove(checked.map { it.file.fileUUID })
            if (result is Success) {
                files.value = files.value.filterNot { it.checked }

                val size = checked.sumOf { it.file.fileSize }
                totalUsage.value = totalUsage.value - size
            }
            refreshing.removeLoader()
        }
    }

    fun uploadFile(action: CloudStorageUIAction.UploadFile) {
        viewModelScope.launch {
            // TODO move to cloudStorageRepository
            var respData: CloudStorageUploadStartResp? = null
            var result = cloudStorageRepository.updateStart(action.info.filename, action.info.size)
            when (result) {
                is Success -> respData = result.data
                is Failure -> {
                    val exception = result.exception as FlatNetException
                    if (exception.code == FlatErrorCode.Web_UploadConcurrentLimit) {
                        cloudStorageRepository.cancel()
                        // retry
                        result = cloudStorageRepository.updateStart(
                            action.info.filename,
                            action.info.size,
                        )
                        if (result is Success) {
                            respData = result.data
                        }
                    }
                }
            }

            if (respData == null) {
                return@launch
            }

            val request = UploadRequest(
                uuid = respData.fileUUID,
                filepath = respData.filePath,
                policy = respData.policy,
                policyURL = respData.policyURL,
                signature = respData.signature,

                filename = action.info.filename,
                size = action.info.size,
                mediaType = action.info.mediaType,
                uri = action.uri
            )
            UploadManager.upload(request)
        }
    }

    private fun handleUploadSuccess(fileUUID: String, filename: String, size: Long) {
        viewModelScope.launch {
            val useProjector = filename.isDynamicDoc() && appKVCenter.useProjectorConvertor()
            val resp = cloudStorageRepository.updateFinish(fileUUID, projector = useProjector)
            if (resp is Success) {
                // delayRemoveSuccess(fileUUID)
                totalUsage.value = totalUsage.value + size
                files.value = files.value.toMutableList().apply {
                    add(0, CloudStorageUIFile(
                        CloudStorageFile(
                            fileUUID = fileUUID,
                            fileName = filename,
                            fileSize = size,
                            fileURL = filename,
                            convertStep = FileConvertStep.None,
                            taskUUID = "",
                            taskToken = "",
                            createAt = System.currentTimeMillis(),
                        )
                    ))
                }
                checkAndStartConvert(filename, fileUUID)
            }
        }
    }

    private fun checkAndStartConvert(filename: String, fileUUID: String) {
        when (filename.coursewareType()) {
            CoursewareType.DocStatic -> {
                startConvert(fileUUID, false)
            }
            CoursewareType.DocDynamic -> {
                val useProjector = appKVCenter.useProjectorConvertor()
                if (useProjector) {
                    startProjectorConvert(fileUUID)
                } else {
                    startConvert(fileUUID, true)
                }
            }
            else -> {; }
        }
    }

    private fun startProjectorConvert(fileUUID: String) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.convertStart(fileUUID, projector = true)
            if (resp is Success) {
                updateConvertStep(fileUUID, FileConvertStep.Converting)
                startProjectorQuery(resp.data.taskUUID, resp.data.taskToken, fileUUID)
            }
        }
    }

    private fun startProjectorQuery(
        taskUUID: String,
        taskToken: String,
        fileUUID: String,
    ) {
        val projectorQuery = ProjectorQuery.Builder()
            .setTaskUuid(taskUUID)
            .setTaskToken(taskToken)
            .setPoolInterval(3000L)
            .setTimeout(120_000)
            .setCallback(object : ProjectorQuery.Callback {
                override fun onProgress(progress: Double, convertInfo: ProjectorQuery.QueryResponse) {
                }

                override fun onFinish(response: ProjectorQuery.QueryResponse) {
                    finishConvert(fileUUID, success = true)
                }

                override fun onFailure(e: ConvertException) {
                    finishConvert(fileUUID, false)
                }
            }).build()
        projectorQuery.startQuery()
    }

    private fun startConvert(fileUUID: String, dynamic: Boolean) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.convertStart(fileUUID)
            if (resp is Success) {
                updateConvertStep(fileUUID, FileConvertStep.Converting)
                startConvertQuery(
                    dynamic,
                    taskUUID = resp.data.taskUUID,
                    taskToken = resp.data.taskToken,
                    fileUUID = fileUUID,
                )
            }
        }
    }

    private fun startConvertQuery(
        dynamic: Boolean,
        taskUUID: String,
        taskToken: String,
        fileUUID: String,
    ) {
        val converterV5 = ConverterV5.Builder().apply {
            setResource("")
            setType(if (dynamic) ConvertType.Dynamic else ConvertType.Static)
            setTaskToken(taskToken)
            setTaskUuid(taskUUID)
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
        files.value.indexOfFirst { fileUUID == it.file.fileUUID }.let { index ->
            if (index < 0) return
            val curFile = files.value[index]

            val changed = curFile.copy(
                file = curFile.file.copy(convertStep = convertStep)
            )
            files.value = files.value.toMutableList().apply { set(index, changed) }
        }
    }

//    private fun delayRemoveSuccess(fileUUID: String) {
//        viewModelScope.launch {
//            delay(3000)
//            uploadFiles.value = uploadFiles.value.toMutableList().filter { it.fileUUID != fileUUID }
//        }
//    }
}

data class CloudStorageUIFile(
    val file: CloudStorageFile,
    val checked: Boolean = false,
)

data class CloudStorageViewState(
    val refreshing: Boolean = false,
    val totalUsage: Long = 0,
    val files: List<CloudStorageUIFile> = emptyList(),
    val uploadFiles: List<UploadFile> = emptyList(),
    val errorMessage: String? = null,
)

sealed class CloudStorageUIAction {
    object Delete : CloudStorageUIAction()
    object Reload : CloudStorageUIAction()
    data class CheckItem(val index: Int, val checked: Boolean) : CloudStorageUIAction()
    data class ClickItem(val file: CloudStorageFile) : CloudStorageUIAction()
    object PreviewRestrict : CloudStorageUIAction()

    object OpenItemPick : CloudStorageUIAction()
    object OpenUploading : CloudStorageUIAction()

    data class UploadFile(val uri: Uri, val info: ContentInfo) : CloudStorageUIAction()
}