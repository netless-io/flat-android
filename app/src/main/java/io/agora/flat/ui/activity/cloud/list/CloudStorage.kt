package io.agora.flat.ui.activity.cloud.list

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.ui.activity.home.EmptyView
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.util.ContentInfo
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.fileIconId
import io.agora.flat.util.showToast

@Composable
fun CloudScreen(
    onOpenUploading: () -> Unit,
    onOpenItemPick: (() -> Unit)? = null,
    viewModel: CloudStorageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    CloudScreen(
        viewState = viewState,
        onOpenUploading = {
            onOpenUploading()
            viewModel.clearBadgeFlag()
        },
        onDeleteClick = viewModel::deleteChecked,
        onReload = viewModel::reloadFileList,
        onOpenItemPick = { onOpenItemPick?.invoke() },
        onUploadFile = viewModel::uploadFile,
        onItemChecked = viewModel::checkItem,
        onItemClick = { file ->
            Navigator.launchPreviewActivity(context, file)
        },
        onPreviewRestrict = {
            context.showToast(R.string.cloud_preview_transcoding_hint)
        }
    )
}

@Composable
internal fun CloudScreen(
    viewState: CloudStorageUiState,
    onOpenUploading: () -> Unit,
    onDeleteClick: () -> Unit,
    onReload: () -> Unit,
    onOpenItemPick: () -> Unit,
    onUploadFile: (uri: Uri, info: ContentInfo) -> Unit,
    onItemChecked: (index: Int, checked: Boolean) -> Unit,
    onItemClick: (file: CloudStorageFile) -> Unit,
    onPreviewRestrict: () -> Unit
) {
    var editMode by rememberSaveable { mutableStateOf(false) }

    Column {
        CloudTopAppBar(
            viewState,
            editMode = editMode,
            onOpenUploading = onOpenUploading,
            onEditClick = { editMode = true },
            onDeleteClick = onDeleteClick,
            onDoneClick = { editMode = false },
        )
        FlatSwipeRefresh(viewState.refreshing, onRefresh = onReload) {
            Box(Modifier.fillMaxSize()) {
                CloudFileList(
                    modifier = Modifier.fillMaxSize(),
                    files = viewState.files,
                    editMode = editMode,
                    onItemChecked = onItemChecked,
                    onItemClick = onItemClick,
                    onPreviewRestrict = onPreviewRestrict
                )
                if (isTabletMode()) {
                    AddFileLayoutPad(onOpenItemPick)
                } else {
                    AddFileLayout(onUploadFile)
                }
            }
        }
    }
}

@Composable
private fun CloudTopAppBar(
    viewState: CloudStorageUiState,
    editMode: Boolean,
    onOpenUploading: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    val deletable = viewState.deletable
    val uploadingSize = viewState.uploadFiles.size
    val showBadge = viewState.showBadge

    val deleteColor = if (deletable) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.error.copy(ContentAlpha.disabled)
    }

    FlatMainTopAppBar(stringResource(R.string.title_cloud_storage)) {
        if (editMode) {
            TextButton(onClick = onDeleteClick, enabled = deletable) {
                FlatTextBodyOne(stringResource(R.string.delete), color = deleteColor)
            }
            TextButton(onClick = onDoneClick) {
                FlatTextBodyOne(stringResource(R.string.done))
            }
        } else {
            UploadingIcon(showBadge, uploadingSize, onClick = onOpenUploading)
            IconButton(onClick = onEditClick) {
                FlatIcon(id = R.drawable.ic_cloud_list_edit)
            }
        }
    }
}

@Composable
private fun UploadingIcon(showBadge: Boolean, size: Int, onClick: () -> Unit) {
    IconButton(onClick = {
        onClick()
    }) {
        BadgedBox(badge = {
            if (showBadge) {
                Badge { Text("$size") }
            }
        }) {
            FlatIcon(id = R.drawable.ic_cloud_list_unloading)
        }
    }
}

@Composable
private fun BoxScope.AddFileLayout(onUploadFile: (uri: Uri, info: ContentInfo) -> Unit) {
    var showPick by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { showPick = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(painterResource(R.drawable.ic_cloud_list_add), contentDescription = null, tint = FlatColorWhite)
    }

    val aniValue: Float by animateFloatAsState(if (showPick) 1f else 0f)
    if (aniValue > 0) {
        UpdatePickLayout(
            aniValue,
            onUploadFile = { uri, info ->
                onUploadFile(uri, info)
                showPick = false
            },
        ) {
            showPick = false
        }
    }
}

@Composable
private fun BoxScope.AddFileLayoutPad(onOpenItemPick: () -> Unit) {
    FloatingActionButton(
        onClick = onOpenItemPick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(painterResource(R.drawable.ic_cloud_list_add), contentDescription = null, tint = FlatColorWhite)
    }
}

@Composable
private fun UpdatePickLayout(
    aniValue: Float,
    onUploadFile: (uri: Uri, info: ContentInfo) -> Unit,
    onCoverClick: () -> Unit
) {
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
            UploadPickRow(onUploadFile)
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
internal fun BoxScope.UploadPickRow(onUploadFile: (uri: Uri, info: ContentInfo) -> Unit) {
    val launcher: (String) -> Unit = launcherPickContent {
        onUploadFile(it.uri, it)
    }
    Row(Modifier.align(Alignment.Center)) {
        UploadPickItem(R.drawable.ic_cloud_file_image, R.string.cloud_storage_upload_image) {
            launcher("image/*")
        }
        UploadPickItem(R.drawable.ic_cloud_file_video, R.string.cloud_storage_upload_video) {
            launcher("video/*")
        }
        UploadPickItem(R.drawable.ic_cloud_file_audio, R.string.cloud_storage_upload_music) {
            launcher("audio/*")
        }
        UploadPickItem(R.drawable.ic_cloud_file_word, R.string.cloud_storage_upload_doc) {
            launcher("*/*")
        }
    }
}

@Composable
private fun RowScope.UploadPickItem(@DrawableRes id: Int, @StringRes text: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1F)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(id), contentDescription = "", Modifier.size(48.dp))
        Spacer(Modifier.height(4.dp))
        FlatTextButton(stringResource(text))
    }
}

@Composable
internal fun CloudFileList(
    modifier: Modifier,
    files: List<CloudUiFile>,
    editMode: Boolean,
    onItemChecked: (index: Int, checked: Boolean) -> Unit,
    onItemClick: (CloudStorageFile) -> Unit,
    onPreviewRestrict: () -> Unit
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(files.firstOrNull()) {
        scrollState.animateScrollToItem(0)
    }

    if (files.isEmpty()) {
        EmptyView(
            modifier = modifier.verticalScroll(rememberScrollState()),
            imgRes = R.drawable.img_cloud_list_empty,
            message = R.string.cloud_storage_no_files
        )
    } else {
        LazyColumn(modifier, state = scrollState) {
            items(
                count = files.size,
                key = { index: Int -> files[index].file.fileUUID },
            ) { index ->
                val item = files[index]
                CloudFileItem(
                    item,
                    editMode,
                    onCheckedChange = { checked ->
                        onItemChecked(index, checked)
                    },
                    onClick = {
                        when (item.file.convertStep) {
                            FileConvertStep.Done, FileConvertStep.None -> {
                                onItemClick(item.file)
                            }
                            else -> onPreviewRestrict()
                        }
                    },
                )
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp), Alignment.TopCenter
                ) {
                    FlatTextCaption(stringResource(R.string.loaded_all))
                }
            }
        }
    }
}

@Composable
private fun CloudFileItem(
    item: CloudUiFile,
    editMode: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit),
    onClick: () -> Unit,
) {
    val file = item.file
    val imageId = file.fileURL.fileIconId()
    Box(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(16.dp))
            Box {
                Image(painterResource(imageId), contentDescription = "", modifier = Modifier.size(24.dp))
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
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                FlatTextBodyOne(
                    file.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = FlatTheme.colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    FlatTextCaption(FlatFormatter.longDate(file.createAt))
                    Spacer(Modifier.width(16.dp))
                    FlatTextCaption(FlatFormatter.size(file.fileSize))
                }
            }
            if (editMode) {
                Checkbox(checked = item.checked, onCheckedChange = onCheckedChange, Modifier.padding(3.dp))
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                IconButton(onClick = { }) {
                    FlatIcon(id = R.drawable.ic_cloud_list_option)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        FlatDivider(startIndent = 52.dp, endIndent = 12.dp, modifier = Modifier.align(Alignment.BottomCenter))
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
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun CloudFileItemPreview() {
    val item = CloudUiFile(
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
    )
    val item2 = CloudUiFile(
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
    )
    FlatPage {
        Column {
            CloudFileItem(item, false, {}, {})
            CloudFileItem(item2, true, {}, {})
        }
    }
}

@Composable
@Preview
private fun CloudStoragePreview() {
    val files = listOf(
        CloudUiFile(
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
        CloudUiFile(
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
        CloudUiFile(
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
    val viewState = CloudStorageUiState(files = files)
    FlatPage {
        CloudScreen(
            viewState,
            { },
            { },
            { },
            { },
            { _, _ -> },
            { _, _ -> },
            {},
            {}
        )
    }
}

@Composable
@Preview
private fun UpdatePickDialogPreview() {
    UpdatePickLayout(1f, { _, _ -> }) {}
}
