package io.agora.flat.ui.activity.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.upload.UploadState
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.fileSuffix
import io.agora.flat.util.showToast

@Composable
fun CloudScreen(
    onOpenUploading: () -> Unit,
    onOpenItemPick: (() -> Unit)? = null,
    viewModel: CloudStorageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    CloudScreen(viewState) { action ->
        when (action) {
            is CloudStorageUIAction.CheckItem -> viewModel.checkItem(action)
            is CloudStorageUIAction.Delete -> viewModel.deleteChecked()
            is CloudStorageUIAction.Reload -> viewModel.reloadFileList()
            is CloudStorageUIAction.UploadFile -> viewModel.uploadFile(action)
            is CloudStorageUIAction.PreviewRestrict -> {
                context.showToast(R.string.cloud_preview_transcoding_hint)
            }

            is CloudStorageUIAction.OpenUploading -> onOpenUploading()
            is CloudStorageUIAction.OpenItemPick -> onOpenItemPick?.invoke()
            is CloudStorageUIAction.ClickItem -> {
                Navigator.launchPreviewActivity(context, action.file)
            }
        }
    }
}

@Composable
internal fun CloudScreen(viewState: CloudStorageViewState, actioner: (CloudStorageUIAction) -> Unit) {
    Column {
        FlatTopAppBar(stringResource(R.string.title_cloud_storage)) {
            UploadIcon(viewState) {
                actioner(CloudStorageUIAction.OpenUploading)
            }
        }
        FlatSwipeRefresh(viewState.refreshing, onRefresh = { actioner(CloudStorageUIAction.Reload) }) {
            Box(Modifier.fillMaxSize()) {
                CloudContent(viewState.totalUsage, viewState.files, actioner)

                if (isTabletMode()) {
                    AddFileLayoutPad(actioner)
                } else {
                    AddFileLayout(actioner)
                }
            }
        }
    }
}

@Composable
private fun UploadIcon(viewState: CloudStorageViewState, onClick: () -> Unit) {
    val showUploadList = viewState.uploadFiles.isNotEmpty()
    val progress = (viewState.uploadFiles.sumOf {
        it.progress.toDouble()
    } / viewState.uploadFiles.size).toFloat()

    val progressColor = if (viewState.uploadFiles.find { it.uploadState == UploadState.Failure } != null)
        FlatColorRed
    else
        FlatColorBlue

    if (showUploadList) {
        IconButton(onClick = onClick) {
            if (progress == 1.0f) Icon(Icons.Outlined.CheckCircleOutline, "", tint = MaterialTheme.colors.primary)
            else FlatCircularProgressIndicator(
                progress = progress,
                color = progressColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.AddFileLayout(actioner: (CloudStorageUIAction) -> Unit) {
    var showPick by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { showPick = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = FlatColorWhite)
    }

    val aniValue: Float by animateFloatAsState(if (showPick) 1f else 0f)
    if (aniValue > 0) {
        UpdatePickLayout(
            aniValue,
            actioner = {
                actioner(it)
                showPick = false
            },
        ) {
            showPick = false
        }
    }
}

@Composable
private fun BoxScope.AddFileLayoutPad(actioner: (CloudStorageUIAction) -> Unit) {
    FloatingActionButton(
        onClick = { actioner(CloudStorageUIAction.OpenItemPick) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = FlatColorWhite)
    }
}

@Composable
private fun UpdatePickLayout(aniValue: Float, actioner: (CloudStorageUIAction) -> Unit, onCoverClick: () -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer(alpha = aniValue)
                .background(Color(0x52000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCoverClick() }) {
        }
        Box(
            MaxWidth
                .height(160.dp * aniValue)
                .background(MaterialTheme.colors.surface)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .clickable { onCoverClick() }) {
                Image(
                    painterResource(R.drawable.ic_record_arrow_down),
                    "",
                    Modifier.padding(4.dp)
                )
            }
            UploadPickRow(actioner)
        }
    }
}

@Composable
fun CloudUploadPick(onPickClose: () -> Unit, viewModel: CloudStorageViewModel = hiltViewModel()) {
    Column {
        CloseTopAppBar(stringResource(id = R.string.title_cloud_pick), onClose = onPickClose)
        Box(MaxWidthSpread) {
            UploadPickRow(viewModel::uploadFile)
        }
    }
}

@Composable
internal fun BoxScope.UploadPickRow(onUploadFile: (CloudStorageUIAction.UploadFile) -> Unit) {
    val launcher: (String) -> Unit = launcherPickContent {
        onUploadFile(CloudStorageUIAction.UploadFile(it.uri, it))
    }
    Row(Modifier.align(Alignment.Center)) {
        UpdatePickItem(R.drawable.ic_cloud_storage_image, R.string.cloud_storage_upload_image) {
            launcher("image/*")
        }
        UpdatePickItem(R.drawable.ic_cloud_storage_video, R.string.cloud_storage_upload_video) {
            launcher("video/*")
        }
        UpdatePickItem(R.drawable.ic_cloud_storage_music, R.string.cloud_storage_upload_music) {
            launcher("audio/*")
        }
        UpdatePickItem(R.drawable.ic_cloud_storage_doc, R.string.cloud_storage_upload_doc) {
            launcher("*/*")
        }
    }
}

@Composable
private fun RowScope.UpdatePickItem(@DrawableRes id: Int, @StringRes text: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1F)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(id), contentDescription = "", Modifier.size(48.dp))
        FlatTextButton(text = stringResource(text))
    }
}

@Composable
internal fun CloudContent(
    totalUsage: Long,
    files: List<CloudStorageUIFile>,
    actioner: (CloudStorageUIAction) -> Unit,
) {
    val checked = files.any { it.checked }

    Column {
        if (files.isEmpty()) {
            EmptyView(
                R.drawable.img_cloud_storage_no_file,
                R.string.cloud_storage_no_files,
                MaxWidthSpread.verticalScroll(rememberScrollState()),
            )
        } else {
            Row(Modifier, verticalAlignment = Alignment.CenterVertically) {
                FlatTextBodyOneSecondary(
                    stringResource(R.string.cloud_storage_usage_format, FlatFormatter.size(totalUsage)),
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    enabled = checked,
                    onClick = { actioner(CloudStorageUIAction.Delete) },
                ) {
                    FlatTextBodyOne(
                        stringResource(R.string.delete),
                        color = if (checked) FlatColorRed else FlatColorRed.copy(alpha = ContentAlpha.disabled),
                    )
                }
            }
            CloudFileList(Modifier.weight(1f), files, actioner)
        }
    }
}

@Composable
private fun CloudFileList(
    modifier: Modifier,
    files: List<CloudStorageUIFile>,
    actioner: (CloudStorageUIAction) -> Unit,
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(files.firstOrNull()) {
        scrollState.animateScrollToItem(0)
    }

    LazyColumn(modifier, state = scrollState) {
        items(
            count = files.size,
            key = { index: Int -> files[index].file.fileUUID },
        ) { index ->
            val item = files[index]
            CloudStorageItem(
                item,
                onCheckedChange = { checked ->
                    actioner(CloudStorageUIAction.CheckItem(index, checked))
                },
                onClick = {
                    when (item.file.convertStep) {
                        FileConvertStep.Done, FileConvertStep.None -> {
                            actioner(CloudStorageUIAction.ClickItem(item.file))
                        }
                        else -> actioner(CloudStorageUIAction.PreviewRestrict)
                    }
                },
            )
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 30.dp), Alignment.TopCenter
            ) {
                FlatTextCaption(stringResource(R.string.loaded_all))
            }
        }
    }
}

@Composable
private fun CloudStorageItem(
    item: CloudStorageUIFile,
    onCheckedChange: ((Boolean) -> Unit),
    onClick: () -> Unit,
) {
    val file = item.file
    // "jpg", "jpeg", "png", "webp","doc", "docx", "ppt", "pptx", "pdf"
    val imageId = when (file.fileURL.fileSuffix()) {
        "jpg", "jpeg", "png", "webp" -> R.drawable.ic_cloud_storage_image
        "ppt", "pptx" -> R.drawable.ic_cloud_storage_ppt
        "pdf" -> R.drawable.ic_cloud_storage_pdf
        "mp4" -> R.drawable.ic_cloud_storage_video
        "mp3", "aac" -> R.drawable.ic_cloud_storage_music
        else -> R.drawable.ic_cloud_storage_doc
    }
    Column(
        Modifier
            .height(68.dp)
            .clickable(onClick = onClick)
    ) {
        Row(MaxWidthSpread, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(12.dp))
            Box {
                Image(painterResource(imageId), contentDescription = "")
                when (item.file.convertStep) {
                    FileConvertStep.Converting -> ConvertingImage(Modifier.align(Alignment.BottomEnd))
                    FileConvertStep.Failed -> Icon(
                        painterResource(R.drawable.ic_cloud_storage_convert_failure),
                        "",
                        Modifier.align(Alignment.BottomEnd),
                        Color.Unspecified,
                    )
                    else -> {; }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                FlatTextBodyTwo(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row {
                    FlatTextCaption(FlatFormatter.dateDash(file.createAt))
                    Spacer(Modifier.width(16.dp))
                    FlatTextCaption(FlatFormatter.size(file.fileSize))
                }
            }
            Checkbox(checked = item.checked, onCheckedChange = onCheckedChange, Modifier.padding(3.dp))
            Spacer(Modifier.width(12.dp))
        }
        FlatDivider(startIndent = 52.dp, endIndent = 12.dp)
    }
}

@Composable
fun ConvertingImage(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()

    val angle: Float by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        )
    )

    Icon(
        painter = painterResource(R.drawable.ic_cloud_storage_converting),
        contentDescription = "",
        modifier.rotate(angle),
        tint = Color.Unspecified
    )
}

@Composable
@Preview
private fun CloudStoragePreview() {
    val files = listOf(
        CloudStorageUIFile(
            CloudStorageFile(
                "1",
                "long long long long long long name file.jpg",
                1111024,
                createAt = 1627898586449,
                fileURL = "",
                convertStep = FileConvertStep.Done,
                taskUUID = "",
                taskToken = "",
            ),
        ),
        CloudStorageUIFile(
            CloudStorageFile(
                "2",
                "2.doc",
                111024,
                createAt = 1627818586449,
                fileURL = "",
                convertStep = FileConvertStep.Done,
                taskUUID = "",
                taskToken = "",
            ),
        ),
        CloudStorageUIFile(
            CloudStorageFile(
                "3",
                "3.mp4",
                111111024,
                createAt = 1617898586449,
                fileURL = "",
                convertStep = FileConvertStep.Done,
                taskUUID = "",
                taskToken = "",
            ),
        ),
    )
    val viewState = CloudStorageViewState(
        files = files
    )
    CloudScreen(viewState) {}
}

@Composable
@Preview
private fun UpdatePickDialogPreview() {
    UpdatePickLayout(1f, {}) {

    }
}
