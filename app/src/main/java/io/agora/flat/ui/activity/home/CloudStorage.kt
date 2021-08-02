package io.agora.flat.ui.activity.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.agora.flat.R
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTopAppBar
import io.agora.flat.ui.theme.*
import io.agora.flat.util.FlatFormatter

@Composable
fun CloudStorage() {
    val viewModel = viewModel(CloudStorageViewModel::class.java)
    val viewState by viewModel.state.collectAsState()

    CloudStorage(viewState) { action ->
        when (action) {
            is CloudStorageUIAction.CheckItem -> viewModel.checkItem(action)
            is CloudStorageUIAction.Delete -> viewModel.deleteChecked()
            is CloudStorageUIAction.Reload -> viewModel.reloadFileList()
            else -> ""
        }
    }
}

@Composable
internal fun CloudStorage(viewState: CloudStorageViewState, actioner: (CloudStorageUIAction) -> Unit) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(viewState.refreshing),
        onRefresh = { actioner(CloudStorageUIAction.Reload) },
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                contentColor = MaterialTheme.colors.primary,
            )
        }) {
        FlatColumnPage {
            FlatCloudStorageTopBar()

            Box(modifier = Modifier.fillMaxSize()) {
                CloudStorageContent(viewState.totalUsage, viewState.files, actioner)

                CloudStorageAddFile()
            }
        }
    }
}

@Composable
fun BoxScope.CloudStorageAddFile() {
    var showPick by remember { mutableStateOf(false) }

    FloatingActionButton(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        onClick = { showPick = true },
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = FlatColorWhite)
    }

    val aniValue: Float by animateFloatAsState(if (showPick) 1f else 0f)
    if (aniValue > 0) {
        UpdatePickLayout(aniValue) {
            showPick = false
        }
    }
}

@Composable
private fun UpdatePickLayout(aniValue: Float, onCoverClick: () -> Unit) {
    Column {
        Box(Modifier
            .weight(1f)
            .fillMaxWidth()
            .graphicsLayer(alpha = aniValue)
            .background(Color(0x52000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCoverClick() }) {
        }
        Box(Modifier
            .height(160.dp * aniValue)
            .fillMaxWidth()
            .background(FlatColorWhite)) {
            Box(Modifier
                .align(Alignment.TopCenter)
                .clickable { onCoverClick() }) {
                Image(
                    painter = painterResource(R.drawable.ic_record_arrow_down),
                    contentDescription = "",
                    Modifier.padding(4.dp)
                )
            }
            Row(Modifier.align(Alignment.Center)) {
                UpdatePickItem(R.drawable.ic_cloud_storage_image, R.string.cloud_storage_upload_image) {

                }
                UpdatePickItem(R.drawable.ic_cloud_storage_video, R.string.cloud_storage_upload_video) {

                }
                UpdatePickItem(R.drawable.ic_cloud_storage_music, R.string.cloud_storage_upload_music) {

                }
                UpdatePickItem(R.drawable.ic_cloud_storage_doc, R.string.cloud_storage_upload_doc) {

                }
            }
        }
    }
}

@Composable
private fun RowScope.UpdatePickItem(@DrawableRes id: Int, @StringRes text: Int, onClick: () -> Unit) {
    Column(Modifier
        .weight(1F)
        .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id), contentDescription = "", Modifier.size(48.dp))
        Text(text = stringResource(text))
    }
}


@Composable
internal fun CloudStorageContent(
    totalUsage: Long,
    files: List<CloudStorageFile>,
    actioner: (CloudStorageUIAction) -> Unit,
) {
    Column {
        Row(Modifier
            .fillMaxWidth()
            .background(FlatColorBackground), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cloud_storage_usage_format, FlatFormatter.size(totalUsage)),
                color = FlatColorTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { actioner(CloudStorageUIAction.Delete) }) {
                Text(text = stringResource(id = R.string.delete), color = Color.Red)
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(count = files.size, key = { index: Int ->
                files[index].fileUUID
            }) { index ->
                CloudStorageItem(files[index],
                    onCheckedChange = { checked -> actioner(CloudStorageUIAction.CheckItem(index, checked)) })
            }
        }
    }
}

@Composable
private fun CloudStorageItem(file: CloudStorageFile, onCheckedChange: ((Boolean) -> Unit)) {
    // "jpg", "jpeg", "png", "webp","doc", "docx", "ppt", "pptx", "pdf"
    val imageId = when (file.fileURL.substringAfterLast('.').toLowerCase()) {
        "jpg", "jpeg", "png", "webp" -> R.drawable.ic_cloud_storage_image
        "ppt", "pptx" -> R.drawable.ic_cloud_storage_ppt
        "pdf" -> R.drawable.ic_cloud_storage_pdf
        "mp4" -> R.drawable.ic_cloud_storage_video
        "mp3", "aac" -> R.drawable.ic_cloud_storage_music
        else -> R.drawable.ic_cloud_storage_doc
    }
    Column(Modifier.height(68.dp)) {
        Row(Modifier
            .fillMaxWidth()
            .weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(12.dp))
            Image(
                painterResource(imageId),
                contentDescription = ""
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = file.fileName)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(text = file.createAt)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = FlatFormatter.size(file.fileSize.toLong()))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(checked = file.checked, onCheckedChange = onCheckedChange, Modifier.padding(3.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Divider(modifier = Modifier.padding(start = 52.dp, end = 12.dp), thickness = 1.dp, color = FlatColorDivider)
    }
}

@Composable
private fun FlatCloudStorageTopBar() {
    FlatTopAppBar(title = { Text(stringResource(id = R.string.title_cloud_storage), style = FlatTitleTextStyle) })
}

@Composable
@Preview
private fun CloudStoragePreview() {
    val files = listOf<CloudStorageFile>(
        CloudStorageFile("1",
            "1.jpg",
            1111024,
            createAt = "17:26:03",
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
        CloudStorageFile("2",
            "2.doc",
            111024,
            createAt = "17:26:03",
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
        CloudStorageFile("3",
            "3.mp4",
            111111024,
            createAt = "17:26:03",
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
    )
    val viewState = CloudStorageViewState(
        files = files
    )
    CloudStorage(viewState) {

    }
}

@Composable
@Preview
private fun UpdatePickDialogPreview() {
    UpdatePickLayout(1f) {

    }
}
