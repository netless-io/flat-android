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
import io.agora.flat.Constants
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.onSuccess
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
    private val appEnv: AppEnv
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
        UploadManager.observeUploadFiles(Constants.UPLOAD_TAG_CLOUD),
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
            UploadManager.observeSuccess(Constants.UPLOAD_TAG_CLOUD).collect {
                handleUploadSuccess(it)
            }
        }

        viewModelScope.launch {
            UploadManager.observeUploadFiles(Constants.UPLOAD_TAG_CLOUD).map { it.size }.distinctUntilChanged().collect {
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
                startConvert(file.fileUUID)
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

    fun delete(fileUUID: String) {
        viewModelScope.launch {
            when (val result = cloudStorageRepository.delete(listOf(fileUUID))) {
                is Success -> {
                    loadedFiles.value = loadedFiles.value.filterNot { it.file.fileUUID == fileUUID }
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
                        ossKey = appEnv.ossKey,

                        filename = info.filename,
                        size = info.size,
                        mediaType = info.mediaType,
                        uri = uri,

                        tag = Constants.UPLOAD_TAG_CLOUD,
                    )
                    UploadManager.upload(request)
                }

                is Failure -> {
                    // TODO
                }
            }
        }
    }

    private fun handleUploadSuccess(r: UploadRequest) {
        viewModelScope.launch {
            cloudStorageRepository.updateFinish(r.uuid).onSuccess {
                totalUsage.value = totalUsage.value + r.size
                val uuid = r.uuid
                val filename = r.filename
                val fileUrl = "${r.policyURL}/${r.filepath}"

                when (filename.coursewareType()) {
                    CoursewareType.DocStatic, CoursewareType.DocDynamic -> {
                        addUiFile(uuid, filename, fileUrl, r.size, null)
                        startConvert(uuid)
                    }

                    else -> {
                        addUiFile(uuid, filename, fileUrl, r.size, ResourceType.NormalResources)
                    }
                }
            }
        }
    }

    private fun addUiFile(
        fileUUID: String,
        filename: String,
        fileUrl: String,
        size: Long,
        resourceType: ResourceType?
    ) {
        val targetFiles = loadedFiles.value.toMutableList()
        val uiFile = CloudUiFile(
            CloudFile(
                fileUUID = fileUUID,
                fileName = filename,
                fileSize = size,
                fileURL = fileUrl,
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
            .setRegion(appEnv.region.toRegion())
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
                            val taskUUID = data.whiteboardConvert!!.taskUUID
                            val taskToken = data.whiteboardConvert.taskToken
                            updateFileState(fileUUID) { file ->
                                createConvertMata(
                                    file = file,
                                    resourceType = ResourceType.WhiteboardConvert,
                                    taskUUID = taskUUID,
                                    taskToken = taskToken
                                )
                            }
                            startConvertQuery(
                                dynamic = false, // only static doc take this branch
                                taskUUID = taskUUID,
                                taskToken = taskToken,
                                fileUUID = fileUUID,
                            )
                        }

                        ResourceType.WhiteboardProjector -> {
                            val taskUUID = data.whiteboardProjector!!.taskUUID
                            val taskToken = data.whiteboardProjector.taskToken
                            updateFileState(fileUUID) { file ->
                                createConvertMata(
                                    file = file,
                                    resourceType = ResourceType.WhiteboardProjector,
                                    taskUUID = taskUUID,
                                    taskToken = data.whiteboardProjector.taskToken
                                )
                            }
                            startProjectorQuery(
                                taskUUID = taskUUID,
                                taskToken = taskToken,
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
            setRegion(appEnv.region.toRegion())
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
            if (resp is Success && success) {
                updateFileState(fileUUID) { file -> transformByConvertStep(file, FileConvertStep.Done) }
            } else {
                updateFileState(fileUUID) { file -> transformByConvertStep(file, FileConvertStep.Failed) }
            }
        }
    }

    private fun updateFileState(fileUUID: String, transform: (CloudFile) -> CloudFile) {
        val targetFiles = loadedFiles.value.toMutableList()
        val index = targetFiles.indexOfFirst { fileUUID == it.file.fileUUID }
        if (index >= 0) {
            val file = targetFiles[index].file
            targetFiles[index] = CloudUiFile(transform(file))
        }
        loadedFiles.value = targetFiles
    }

    private fun transformByConvertStep(file: CloudFile, convertStep: FileConvertStep): CloudFile {
        val result: CloudFile = when (file.resourceType) {
            ResourceType.WhiteboardConvert -> {
                val fileMeta = file.meta?.copy(
                    whiteboardConvert = file.whiteboardConvert.copy(convertStep = convertStep)
                )
                file.copy(meta = fileMeta)
            }

            ResourceType.WhiteboardProjector -> {
                val fileMeta = file.meta?.copy(
                    whiteboardProjector = file.whiteboardProjector.copy(convertStep = convertStep)
                )
                file.copy(meta = fileMeta)
            }

            else -> file
        }
        return result
    }

    private fun createConvertMata(
        file: CloudFile,
        resourceType: ResourceType,
        taskUUID: String,
        taskToken: String
    ): CloudFile {
        val meta = when (resourceType) {
            ResourceType.WhiteboardConvert -> {
                CloudFileMeta(
                    whiteboardConvert = WhiteboardConvertPayload(
                        "",
                        convertStep = FileConvertStep.Converting,
                        taskUUID = taskUUID,
                        taskToken = taskToken
                    )
                )
            }

            ResourceType.WhiteboardProjector -> {
                CloudFileMeta(
                    whiteboardProjector = WhiteboardProjectorPayload(
                        "",
                        convertStep = FileConvertStep.Converting,
                        taskUUID = taskUUID,
                        taskToken = taskToken
                    )
                )
            }

            else -> null
        }
        return file.copy(resourceType = resourceType, meta = meta)
    }

    fun clearBadgeFlag() {
        showBadge.value = false
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            when (val result = cloudStorageRepository.createDirectory(state.value.dirPath, name)) {
                is Success -> {
                    addUiFile(UUID.randomUUID().toString(), name, "", 0, ResourceType.Directory)
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
                is Success -> updateFileState(fileUuid) { file ->
                    file.copy(fileName = fileName)
                }

                is Failure -> {
                }
            }
        }
    }
}

