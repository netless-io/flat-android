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
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
) : ViewModel() {
    private val loadUiState = MutableStateFlow(LoadUiState.Init)
    private val loadedFiles = MutableStateFlow(listOf<CloudUiFile>())
    private val totalUsage = MutableStateFlow(0L)

    private val showBadge = MutableStateFlow(false)
    private val dirPath = MutableStateFlow(CLOUD_ROOT_DIR)

    val state = combine(
        loadUiState,
        showBadge,
        totalUsage,
        loadedFiles,
        UploadManager.uploadFiles,
        dirPath,
    ) { loadUiState, showBadge, totalUsage, files, uploadFiles, dirPath ->
        CloudStorageUiState(
            loadUiState = loadUiState,
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
            state.map { it.dirPath }.distinctUntilChanged().collect {
                reloadFileList()
            }
        }

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
    }

    fun reloadFileList() {
        viewModelScope.launch {
            loadUiState.value = loadUiState.value.copy(refresh = LoadState.Loading)
            when (val resp = cloudStorageRepository.listFiles(1, path = state.value.dirPath)) {
                is Success -> {
                    totalUsage.value = resp.data.totalUsage
                    val files = resp.data.files
                    loadedFiles.value = files.map { CloudUiFile(it) }
                    files.forEach(::checkConvertState)
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(
                            page = 1,
                            refresh = LoadState.NotLoading(files.isEmpty()),
                            append = LoadState.NotLoading(false)
                        )
                    }
                }
                is Failure -> {
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(
                            page = 0,
                            refresh = LoadState.Error(resp.exception),
                            append = LoadState.NotLoading(false)
                        )
                    }
                }
            }
        }
    }

    fun loadMoreFileList() {
        viewModelScope.launch {
            if (loadUiState.value.append == LoadState.Loading) {
                return@launch
            }
            loadUiState.value = loadUiState.value.copy(append = LoadState.Loading)
            val nextPage = loadUiState.value.page + 1
            when (val resp = cloudStorageRepository.listFiles(nextPage, path = state.value.dirPath)) {
                is Success -> {
                    totalUsage.value = resp.data.totalUsage
                    val files = resp.data.files
                    loadedFiles.value += files.map { CloudUiFile(it) }
                    files.forEach(::checkConvertState)
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(
                            page = nextPage,
                            append = LoadState.NotLoading(files.isEmpty())
                        )
                    }
                }
                is Failure -> {
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(append = LoadState.Error(resp.exception))
                    }
                }
            }
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
            val fs = loadedFiles.value.toMutableList()
            fs[index] = fs[index].copy(checked = checked)
            loadedFiles.value = fs
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            val checked = loadedFiles.value.filter { it.checked }
            when (val result = cloudStorageRepository.delete(checked.map { it.file.fileUUID })) {
                is Success -> {
                    loadedFiles.value = loadedFiles.value.filterNot { it.checked }
                    val size = checked.sumOf { it.file.fileSize }
                    totalUsage.value = totalUsage.value - size
                }
                is Failure -> {

                }
            }
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
        val targetFiles = loadedFiles.value.toMutableList()
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
        loadedFiles.value = targetFiles
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
        val targetFiles = loadedFiles.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(copyConvertStep(file, convertStep))
        }
        loadedFiles.value = targetFiles
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
        val targetFiles = loadedFiles.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(file.copy(resourceType = resourceType))
        }
        loadedFiles.value = targetFiles
    }

    private fun updateFilename(fileUUID: String, filename: String) {
        val targetFiles = loadedFiles.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(file.copy(fileName = filename))
        }
        loadedFiles.value = targetFiles
    }

    fun clearBadgeFlag() {
        showBadge.value = false
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            when (val result = cloudStorageRepository.createDirectory(state.value.dirPath, name)) {
                is Success -> {
                    addUiFile(UUID.randomUUID().toString(), name, 0, ResourceType.Directory)
                }
                is Failure -> {

                }
            }
        }
    }

    fun enterFolder(name: String) {
        dirPath.value = "${state.value.dirPath}$name/"
    }

    fun backFolder() {
        dirPath.value = state.value.dirPath.parentFolder()
    }

    fun rename(fileUuid: String, fileName: String) {
        viewModelScope.launch {
            val name = fileName.nameWithoutExtension()
            when (val result = cloudStorageRepository.rename(fileUuid, name)) {
                is Success -> updateFilename(fileUuid, fileName)
                is Failure -> {
                }
            }
        }
    }
}

