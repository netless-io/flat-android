package io.agora.flat.ui.activity.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.flat.R
import io.agora.flat.common.upload.UploadState
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.theme.*
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun UploadList() {
    val viewModel = viewModel(CloudStorageViewModel::class.java)
    val viewState by viewModel.state.collectAsState()

    UploadList(viewState) { action ->
        when (action) {
            is CloudStorageUIAction.UploadRetry -> viewModel.retryUpload(action.fileUUID)
            is CloudStorageUIAction.UploadCancel -> viewModel.cancelUpload(action.fileUUID)
            is CloudStorageUIAction.UploadDelete -> viewModel.deleteUpload(action.fileUUID)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun UploadList(viewState: CloudStorageViewState, actioner: (CloudStorageUIAction) -> Unit) {
    var showList by remember { mutableStateOf(false) }

    val progress = max(viewState.uploadFiles.sumOf {
        it.progress.toDouble()
    } / viewState.uploadFiles.size, 0.1).toFloat()

    val progressColor = if (viewState.uploadFiles.find { it.uploadState == UploadState.Failure } != null)
        FlatColorRed
    else
        FlatColorBlue

    Box(Modifier.fillMaxWidth()) {
        if (viewState.uploadFiles.isNotEmpty()) {
            Box(Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)) {
                IconButton(onClick = { showList = true }) {
                    CircularProgressIndicator(progress = progress,
                        color = progressColor,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
        AnimatedVisibility(
            visible = showList,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(color = MaterialTheme.colors.background) {
                Column(Modifier.fillMaxSize()) {
                    CloseTopAppBar(title = "传输列表", onClose = { showList = false })
                    UploadListContent(viewState.uploadFiles, actioner)
                }
            }
        }
    }
}

@Composable
fun UploadListContent(uploadFiles: List<UploadFile>, actioner: (CloudStorageUIAction) -> Unit) {
    LazyColumn {
        items(count = uploadFiles.size, key = { index: Int ->
            uploadFiles[index].fileUUID
        }) { index ->
            SwipeDeleteUploadListItem(uploadFiles[index], actioner)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeDeleteUploadListItem(
    uploadFile: UploadFile,
    actioner: (CloudStorageUIAction) -> Unit,
) = BoxWithConstraints {
    val width = constraints.maxWidth.toFloat()
    val squareSize = 56.dp
    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { squareSize.toPx() }
    val anchors = mapOf(width to 0, (width - sizePx) to 1)

    Box(
        modifier = Modifier.swipeable(
            state = swipeableState,
            anchors = anchors,
            thresholds = { _, _ -> FractionalThreshold(0.3f) },
            orientation = Orientation.Horizontal
        )
    ) {
        Box(modifier = Modifier
            .offset { IntOffset((swipeableState.offset.value - width).roundToInt(), 0) }) {
            UploadListItem(uploadFile, actioner)
        }
        Row(Modifier.offset { IntOffset((swipeableState.offset.value).roundToInt(), 0) }) {
            Box(Modifier
                .width(squareSize)
                .fillMaxHeight(),
                Alignment.Center) {
                IconButton(onClick = { actioner(CloudStorageUIAction.UploadDelete(uploadFile.fileUUID)) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "")
                }
            }
        }
    }
}

@Composable
private fun UploadListItem(uploadFile: UploadFile, actioner: (CloudStorageUIAction) -> Unit) {
    val info = when (uploadFile.uploadState) {
        UploadState.Success -> "上传成功"
        UploadState.Failure -> "上传失败"
        else -> "${(uploadFile.progress * 100).toInt()}%"
    }.toString()

    val infoColor = when (uploadFile.uploadState) {
        UploadState.Success -> FlatColorLightGreen
        UploadState.Failure -> FlatColorRed
        else -> FlatColorTextSecondary
    }

    val progressColor = if (uploadFile.uploadState == UploadState.Failure) FlatColorRed else FlatColorBlue

    Box(Modifier.height(50.dp)) {
        Row(Modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                uploadFile.fileName,
                Modifier.weight(1f),
                style = FlatCommonTextStyle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = info,
                color = infoColor,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(4.dp))

            UploadControlImageButton(uploadFile, actioner)
        }
        LinearProgressIndicator(progress = uploadFile.progress,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .align(Alignment.BottomCenter), color = progressColor)
    }
}

@Composable
private fun UploadControlImageButton(uploadFile: UploadFile, actioner: (CloudStorageUIAction) -> Unit) {
    when (uploadFile.uploadState) {
        UploadState.Success -> Image(painterResource(R.drawable.ic_upload_success), contentDescription = "")
        UploadState.Failure -> Image(painterResource(R.drawable.ic_upload_retry),
            contentDescription = "",
            Modifier.clickable { actioner(CloudStorageUIAction.UploadRetry(uploadFile.fileUUID)) })
        else -> Image(painterResource(R.drawable.ic_upload_cancel),
            contentDescription = "",
            Modifier.clickable { actioner(CloudStorageUIAction.UploadCancel(uploadFile.fileUUID)) })
    }
}

@Composable
@Preview
internal fun UploadListPreview() {
    val uploadFiles = listOf<UploadFile>(
        UploadFile("111", "11111111111111111111111111111111111111111111.jpg"),
        UploadFile("222", "222.jpg", UploadState.Uploading, 0.2f),
        UploadFile("333", "333.jpg", UploadState.Success, 1f),
        UploadFile("444", "444.jpg", UploadState.Failure, progress = 0.6f),
    )
    val viewState = CloudStorageViewState(uploadFiles = uploadFiles)
    FlatPage {
        UploadList(viewState) {

        }
    }
}