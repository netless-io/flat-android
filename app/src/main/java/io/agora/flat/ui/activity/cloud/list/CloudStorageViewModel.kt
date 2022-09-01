package io.agora.flat.ui.activity.cloud.list

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
import kotlinx.coroutines.flow.*
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
    private val files = MutableStateFlow(listOf<CloudUiFile>())
    private val totalUsage = MutableStateFlow(0L)
    private val refreshing = ObservableLoadingCounter()
    private val showBadge = MutableStateFlow(false)

    val state = combine(
        refreshing.observable,
        showBadge,
        totalUsage,
        files,
        UploadManager.uploadFiles,
    ) { refreshing, showBadge, totalUsage, files, uploadFiles ->
        CloudStorageUiState(
            refreshing = refreshing,
            showBadge = showBadge,
            totalUsage = totalUsage,
            files = files,
            uploadFiles = uploadFiles,
            errorMessage = null,
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CloudStorageUiState(),
    )

    init {
        viewModelScope.launch {
            UploadManager.uploadSuccess.filterNotNull().collect {
                handleUploadSuccess(fileUUID = it.uuid, filename = it.filename, size = it.size)
            }
        }

        viewModelScope.launch {
            UploadManager.uploadFiles.map { it.size }.distinctUntilChanged().collect {
                showBadge.value = it > 0
            }
        }

        reloadFileList()
    }

    fun reloadFileList() {
        viewModelScope.launch {
            refreshing.addLoader()
            runAtLeast {
                val resp = cloudStorageRepository.listFiles(1)
                if (resp is Success) {
                    totalUsage.value = resp.data.totalUsage
                    files.value = resp.data.files.map {
                        CloudUiFile(file = it, checked = false)
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

    fun checkItem(index: Int, checked: Boolean) {
        viewModelScope.launch {
            val fs = files.value.toMutableList()
            fs[index] = fs[index].copy(checked = checked)
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

    fun uploadFile(uri: Uri, info: ContentInfo) {
        viewModelScope.launch {
            // TODO move to cloudStorageRepository
            var respData: CloudStorageUploadStartResp? = null
            var result = cloudStorageRepository.updateStart(info.filename, info.size)
            when (result) {
                is Success -> respData = result.data
                is Failure -> {
                    val exception = result.exception as FlatNetException
                    if (exception.code == FlatErrorCode.Web_UploadConcurrentLimit) {
                        cloudStorageRepository.cancel()
                        // retry
                        result = cloudStorageRepository.updateStart(
                            info.filename,
                            info.size,
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

                filename = info.filename,
                size = info.size,
                mediaType = info.mediaType,
                uri = uri
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
                    add(
                        0, CloudUiFile(
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
                        )
                    )
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
            val uiFile = files.value[index]

            val changed = uiFile.copy(
                file = uiFile.file.copy(convertStep = convertStep)
            )
            files.value = files.value.toMutableList().apply { set(index, changed) }
        }
    }

    fun clearBadgeFlag() {
        showBadge.value = false
    }
}

