package io.agora.flat.ui.activity.play

import android.graphics.BitmapFactory
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
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.ui.activity.cloud.list.LoadUiState
import io.agora.flat.util.coursewareType
import io.agora.flat.util.delayLaunch
import io.agora.flat.util.parentFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassCloudViewModel @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val boardRoom: BoardRoom,
) : ViewModel() {
    private val dirPath = MutableStateFlow(CLOUD_ROOT_DIR)
    private val loadUiState = MutableStateFlow(LoadUiState.Init)
    private val loadedFiles = MutableStateFlow(listOf<CloudFile>())

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
    }

    fun reloadFileList() {
        viewModelScope.launch {
            loadUiState.value = loadUiState.value.copy(refresh = LoadState.Loading)
            when (val resp = cloudStorageRepository.listFiles(1, path = state.value.dirPath)) {
                is Success -> {
                    val files = resp.data.files
                    loadedFiles.value = files
                    delayLaunch {
                        loadUiState.value = loadUiState.value.copy(
                            page = 1,
                            refresh = LoadState.NotLoading(files.isEmpty()),
                            append = LoadState.NotLoading.Incomplete
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

    fun insertCourseware(file: CloudFile) {
        viewModelScope.launch {
            // "正在插入课件……"
            when (file.fileURL.coursewareType()) {
                CoursewareType.Image -> {
                    val imageSize = loadImageSize(file.fileURL)
                    boardRoom.insertImage(file.fileURL, w = imageSize.width, h = imageSize.height)
                }
                CoursewareType.Audio, CoursewareType.Video -> {
                    boardRoom.insertVideo(file.fileURL, file.fileName)
                }
                CoursewareType.DocStatic -> {
                    insertDocs(file, false)
                }
                CoursewareType.DocDynamic -> {
                    if (file.resourceType == ResourceType.WhiteboardConvert) {
                        insertDocs(file, true)
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

    /**
     * This code is used as an example, the application needs to manage io and async itself.
     * The application may get the image width and height from the api
     *
     * @param src
     */
    private suspend fun loadImageSize(src: String): ImageSize {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                URL(src).openStream().use {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(it, null, options)
                    ImageSize(options.outWidth, options.outHeight)
                }
            } catch (e: IOException) {
                ImageSize(720, 360)
            }
        }
    }

    private fun insertDocs(file: CloudFile, dynamic: Boolean) {
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
                    boardRoom.insertPpt("/${file.whiteboardConvert.taskUUID}/${UUID.randomUUID()}", ppt, file.fileName)
                }

                override fun onFailure(e: ConvertException) {
                }
            })
        }.build()
        convert.startConvertTask()
    }

    private fun insertProjectorDocs(file: CloudFile) {
        val projectorQuery = ProjectorQuery.Builder()
            .setTaskToken(file.whiteboardProjector.taskToken)
            .setTaskUuid(file.whiteboardProjector.taskUUID)
            .setPoolInterval(2000)
            .setCallback(object : ProjectorQuery.Callback {
                override fun onProgress(progress: Double, convertInfo: ProjectorQuery.QueryResponse) {

                }

                override fun onFinish(response: ProjectorQuery.QueryResponse) {
                    boardRoom.insertProjectorPpt(file.whiteboardProjector.taskUUID, response.prefix, file.fileName)
                }

                override fun onFailure(e: ConvertException?) {

                }
            })
            .build()
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