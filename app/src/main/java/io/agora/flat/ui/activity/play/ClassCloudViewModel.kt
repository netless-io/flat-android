package io.agora.flat.ui.activity.play

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herewhite.sdk.ConverterCallbacks
import com.herewhite.sdk.converter.ConvertType
import com.herewhite.sdk.converter.ConverterV5
import com.herewhite.sdk.converter.ProjectorQuery
import com.herewhite.sdk.domain.ConversionInfo
import com.herewhite.sdk.domain.ConvertException
import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.PptPage
import com.herewhite.sdk.domain.Scene
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.board.AgoraBoardRoom
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.TakePhotoEvent
import io.agora.flat.http.model.CloudUploadStartResp
import io.agora.flat.ui.activity.cloud.list.LoadUiState
import io.agora.flat.ui.manager.RoomErrorManager
import io.agora.flat.util.ContentInfo
import io.agora.flat.util.coursewareType
import io.agora.flat.util.delayLaunch
import io.agora.flat.util.parentFolder
import io.agora.flat.util.toRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassCloudViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val boardRoom: AgoraBoardRoom,
    private val eventbus: EventBus,
    private val roomErrorManager: RoomErrorManager,
    private val appEnv: AppEnv,
) : ViewModel() {
    private val dirPath = MutableStateFlow(CLOUD_ROOT_DIR)
    private val loadUiState = MutableStateFlow(LoadUiState.Init)
    private val loadedFiles = MutableStateFlow(listOf<CloudFile>())

    private val uploadingFiles = mutableMapOf<String, CloudUploadStartResp>()

    val state = combine(
        loadUiState,
        loadedFiles,
        dirPath,
    ) { loadUiState, files, dirPath ->
        ClassCloudUiState(
            loadUiState = loadUiState,
            files = files,
            dirPath = dirPath,
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClassCloudUiState(),
    )

    init {
        viewModelScope.launch {
            state.map { it.dirPath }.distinctUntilChanged().collect {
                reloadFileList()
            }
        }

        viewModelScope.launch {
            eventbus.events.filterIsInstance<TakePhotoEvent>().collect {
                insertTakePhoto(it.info)
            }
        }

        viewModelScope.launch {
            UploadManager.observeSuccess(Constants.UPLOAD_TAG_TAKE_PHOTO).collect {
                handleUploadSuccess(it)
            }
        }
    }

    private fun handleUploadSuccess(it: UploadRequest) {
        viewModelScope.launch {
            // insert image by camera image
            uploadingFiles[it.uuid]?.run {
                val fileUrl = "${ossDomain}/${ossFilePath}"
                cloudStorageRepository.uploadTempFileFinish(it.uuid)
                    .onSuccess {
                        insertImage(fileUrl)
                    }.onFailure {
                        roomErrorManager.notifyError("insert photo error", it)
                    }
                uploadingFiles.remove(it.uuid)
            }
        }

    }

    fun reloadFileList() {
        viewModelScope.launch {
            loadUiState.value = loadUiState.value.copy(refresh = LoadState.Loading)
            when (val resp = cloudStorageRepository.listFiles(1, path = state.value.dirPath)) {
                is Success -> {
                    val files = resp.data.files
                    loadedFiles.value = files
                    delayLaunch {
                        // when response size less than request size, complete
                        val appendState = if (files.size < 20) {
                            LoadState.NotLoading.Complete
                        } else {
                            LoadState.NotLoading.Incomplete
                        }
                        loadUiState.value = loadUiState.value.copy(
                            page = 1,
                            refresh = LoadState.NotLoading(files.isEmpty()),
                            append = appendState
                        )
                    }
                }
                is Failure -> {
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(
                            page = 0,
                            refresh = LoadState.Error(resp.exception),
                            append = LoadState.NotLoading.Complete
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
                    val files = resp.data.files
                    loadedFiles.value += files
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

    private fun insertTakePhoto(info: ContentInfo) {
        viewModelScope.launch {
            cloudStorageRepository.uploadTempFileStart(
                info.filename,
                info.size
            ).onSuccess { data ->
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
                    uri = info.uri,

                    tag = Constants.UPLOAD_TAG_TAKE_PHOTO,
                )
                uploadingFiles[data.fileUUID] = data
                UploadManager.upload(request)
            }.onFailure {
                roomErrorManager.notifyError("upload file error", it)
            }
        }
    }

    fun insertCourseware(file: CloudFile) {
        viewModelScope.launch {
            when (file.fileURL.coursewareType()) {
                CoursewareType.Image -> {
                    insertImage(file.fileURL)
                }

                CoursewareType.Audio, CoursewareType.Video -> {
                    boardRoom.insertVideo(file.fileURL, file.fileName)
                }

                CoursewareType.DocStatic -> {
                    if (file.resourceType == ResourceType.WhiteboardConvert) {
                        insertV5Docs(file, false)
                    } else if (file.resourceType == ResourceType.WhiteboardProjector) {
                        insertProjectorDocs(file)
                    }
                }

                CoursewareType.DocDynamic -> {
                    if (file.resourceType == ResourceType.WhiteboardConvert) {
                        insertV5Docs(file, true)
                    } else if (file.resourceType == ResourceType.WhiteboardProjector) {
                        insertProjectorDocs(file)
                    }
                }

                else -> {
                    // Not Support Mobile
                }
            }
        }
    }

    private suspend fun insertImage(fileUrl: String) {
        val imageInfo = loadImageInfo(fileUrl)
        val change =
            imageInfo.orientation == ExifInterface.ORIENTATION_ROTATE_90 || imageInfo.orientation == ExifInterface.ORIENTATION_ROTATE_270
        if (change) {
            boardRoom.insertImage(fileUrl, w = imageInfo.height, h = imageInfo.width)
        } else {
            boardRoom.insertImage(fileUrl, w = imageInfo.width, h = imageInfo.height)
        }
    }

    /**
     * This code is used as an example, the application needs to manage io and async itself.
     * The application may get the image width and height from the api
     *
     * @param src
     */
    private suspend fun loadImageInfo(src: String): ImageInfo {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                URL(src).openStream().use {
                    val bos = ByteArrayOutputStream().apply {
                        it.copyTo(this)
                    }
                    val firstClone = ByteArrayInputStream(bos.toByteArray())
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(firstClone, null, options)
                    firstClone.close()

                    val secondClone = ByteArrayInputStream(bos.toByteArray())
                    val exifInterface = ExifInterface(secondClone)
                    val orientation = exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    secondClone.close()
                    ImageInfo(options.outWidth, options.outHeight, orientation)
                }
            } catch (e: IOException) {
                ImageInfo(720, 360, ExifInterface.ORIENTATION_NORMAL)
            }
        }
    }

    private fun insertV5Docs(file: CloudFile, dynamic: Boolean) {
        val convert = ConverterV5.Builder().apply {
            setResource(file.fileURL)
            setType(if (dynamic) ConvertType.Dynamic else ConvertType.Static)
            setTaskUuid(file.whiteboardConvert.taskUUID)
            setTaskToken(file.whiteboardConvert.taskToken)
            setPoolInterval(2000)
            setCallback(object : ConverterCallbacks {
                override fun onProgress(progress: Double, convertInfo: ConversionInfo?) {
                }

                override fun onFinish(ppt: ConvertedFiles, convertInfo: ConversionInfo) {
                    boardRoom.insertPpt(
                        "/${file.whiteboardConvert.taskUUID}/${UUID.randomUUID()}",
                        ppt.scenes.toList(),
                        file.fileName
                    )
                }

                override fun onFailure(e: ConvertException) {
                }
            })
        }.build()
        convert.startConvertTask()
    }

    private fun ProjectorQuery.QueryResponse.scenes(): List<Scene> {
        return IntRange(1, images.size).mapNotNull { index ->
            images["$index"]?.let {
                Scene("$index", PptPage(it.url, it.width.toDouble(), it.height.toDouble()))
            }
        }
    }

    private fun ProjectorQuery.QueryResponse.isStatic() = type == "static"

    private fun insertProjectorDocs(file: CloudFile) {
        val projectorQuery = ProjectorQuery.Builder()
            .setTaskUuid(file.whiteboardProjector.taskUUID)
            .setTaskToken(file.whiteboardProjector.taskToken)
            .setRegion(appEnv.region.toRegion())
            .setPoolInterval(2000)
            .setCallback(object : ProjectorQuery.Callback {
                override fun onProgress(progress: Double, convertInfo: ProjectorQuery.QueryResponse) {

                }

                override fun onFinish(response: ProjectorQuery.QueryResponse) {
                    if (response.isStatic()) {
                        boardRoom.insertPpt(
                            "/${response.uuid}/${UUID.randomUUID()}",
                            response.scenes(),
                            file.fileName
                        )
                    } else {
                        boardRoom.insertProjectorPpt(file.whiteboardProjector.taskUUID, response.prefix, file.fileName)
                    }
                }

                override fun onFailure(e: ConvertException?) {

                }
            }).build()
        projectorQuery.startQuery()
    }

    fun enterFolder(name: String) {
        dirPath.value = "${state.value.dirPath}$name/"
    }

    fun backFolder() {
        dirPath.value = state.value.dirPath.parentFolder()
    }
}

data class ClassCloudUiState(
    val loadUiState: LoadUiState = LoadUiState.Init,
    val files: List<CloudFile> = emptyList(),
    val dirPath: String = CLOUD_ROOT_DIR,
)