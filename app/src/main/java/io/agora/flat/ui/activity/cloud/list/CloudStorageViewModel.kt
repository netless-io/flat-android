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
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.CoursewareType
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.data.model.ResourceType
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
) : ViewModel() {
    private val files = MutableStateFlow(listOf<CloudUiFile>())
    private val totalUsage = MutableStateFlow(0L)
    private val refreshing = ObservableLoadingCounter()
    private val showBadge = MutableStateFlow(false)
    private val dirPath = MutableStateFlow("/")

    val state = combine(
        refreshing.observable,
        showBadge,
        totalUsage,
        files,
        UploadManager.uploadFiles,
        dirPath,
    ) { refreshing, showBadge, totalUsage, files, uploadFiles, dirPath ->
        CloudStorageUiState(
            refreshing = refreshing,
            showBadge = showBadge,
            totalUsage = totalUsage,
            files = files,
            uploadFiles = uploadFiles,
            dirPath = dirPath,
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
                val resp = cloudStorageRepository.listFiles(1, path = state.value.dirPath)
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

    private fun checkConvertState(file: CloudFile) {
        when (file.convertStep) {
            FileConvertStep.None -> {
                startConvert(file.fileURL)
            }
            FileConvertStep.Converting -> {
                if (file.resourceType == ResourceType.WhiteboardConvert) {
                    startConvertQuery(
                        dynamic = file.fileURL.isDynamicDoc(),
                        taskUUID = file.whiteboardConvert.taskToken,
                        taskToken = file.whiteboardConvert.taskUUID,
                        fileUUID = file.fileUUID
                    )
                }
                if (file.resourceType == ResourceType.WhiteboardProjector) {
                    startProjectorQuery(
                        taskUUID = file.whiteboardProjector.taskUUID,
                        taskToken = file.whiteboardProjector.taskToken,
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
            val result = cloudStorageRepository.delete(checked.map { it.file.fileUUID })
            if (result is Success) {
                files.value = files.value.filterNot { it.checked }
                val size = checked.sumOf { it.file.fileSize }
                totalUsage.value = totalUsage.value - size
            } else {

            }
            refreshing.removeLoader()
        }
    }

    fun uploadFile(uri: Uri, info: ContentInfo) {
        viewModelScope.launch {
            when (val result = cloudStorageRepository.updateStart(info.filename, info.size, state.value.dirPath)) {
                is Success -> {
                    val data = result.data
                    val request = UploadRequest(
                        uuid = data.fileUUID,
                        policyURL = data.ossDomain,
                        filepath = data.ossFilePath,
                        policy = data.policy,
                        signature = data.signature,

                        filename = info.filename,
                        size = info.size,
                        mediaType = info.mediaType,
                        uri = uri
                    )
                    UploadManager.upload(request)
                }
                is Failure -> {
                    // TODO
                }
            }
        }
    }

    private fun handleUploadSuccess(fileUUID: String, filename: String, size: Long) {
        viewModelScope.launch {
            val resp = cloudStorageRepository.updateFinish(fileUUID)
            if (resp is Success) {
                totalUsage.value = totalUsage.value + size
                when (filename.coursewareType()) {
                    CoursewareType.DocStatic, CoursewareType.DocDynamic -> {
                        addUiFile(fileUUID, filename, size, null)
                        startConvert(fileUUID)
                    }
                    else -> {
                        addUiFile(fileUUID, filename, size, ResourceType.NormalResources)
                    }
                }
            }
        }
    }

    private fun addUiFile(fileUUID: String, filename: String, size: Long, resourceType: ResourceType?) {
        val targetFiles = files.value.toMutableList()
        val uiFile = CloudUiFile(
            CloudFile(
                fileUUID = fileUUID,
                fileName = filename,
                fileSize = size,
                fileURL = filename,
                createAt = System.currentTimeMillis(),
                resourceType = resourceType,
            )
        )
        targetFiles.add(0, uiFile)
        files.value = targetFiles
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

    private fun startConvert(fileUUID: String) {
        viewModelScope.launch {
            when (val resp = cloudStorageRepository.convertStart(fileUUID)) {
                is Success -> {
                    val data = resp.data
                    when (data.resourceType) {
                        ResourceType.WhiteboardConvert -> {
                            updateResourceType(fileUUID, ResourceType.WhiteboardConvert)
                            updateConvertStep(fileUUID, FileConvertStep.Converting)
                            startConvertQuery(
                                false, // only static doc take this branch
                                taskUUID = data.whiteboardConvert!!.taskUUID,
                                taskToken = data.whiteboardConvert.taskToken,
                                fileUUID = fileUUID,
                            )
                        }
                        ResourceType.WhiteboardProjector -> {
                            updateResourceType(fileUUID, ResourceType.WhiteboardProjector)
                            updateConvertStep(fileUUID, FileConvertStep.Converting)
                            startProjectorQuery(
                                taskUUID = data.whiteboardProjector!!.taskUUID,
                                taskToken = data.whiteboardProjector.taskToken,
                                fileUUID = fileUUID,
                            )
                        }
                        else -> {}
                    }

                }
                is Failure -> {
                    // TODO
                }
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
        val targetFiles = files.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(copyConvertStep(file, convertStep))
        }
        files.value = targetFiles
    }

    private fun copyConvertStep(file: CloudFile, convertStep: FileConvertStep): CloudFile {
        val result: CloudFile = when (file.resourceType) {
            ResourceType.WhiteboardConvert -> {
                val fileMeta = file.meta!!.copy(
                    whiteboardConvert = file.whiteboardConvert.copy(convertStep = convertStep)
                )
                file.copy(meta = fileMeta)
            }
            ResourceType.WhiteboardProjector -> {
                val fileMeta = file.meta!!.copy(
                    whiteboardProjector = file.whiteboardProjector.copy(convertStep = convertStep)
                )
                file.copy(meta = fileMeta)
            }
            else -> file
        }
        return result
    }

    private fun updateResourceType(fileUUID: String, resourceType: ResourceType) {
        val targetFiles = files.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(file.copy(resourceType = resourceType))
        }
        files.value = targetFiles
    }

    fun clearBadgeFlag() {
        showBadge.value = false
    }
}

