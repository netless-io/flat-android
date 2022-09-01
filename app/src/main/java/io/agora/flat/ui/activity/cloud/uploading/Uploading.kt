package io.agora.flat.ui.activity.cloud.uploading

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.R
import io.agora.flat.common.upload.UploadFile
import io.agora.flat.common.upload.UploadState
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.fileSuffix

@Composable
internal fun Uploading(onCloseUploading: () -> Unit, viewModel: UploadingViewModel = hiltViewModel()) {
    val viewState by viewModel.state.collectAsState()

    Uploading(viewState, onCloseUploading, viewModel::retryUpload)
}

@Composable
internal fun Uploading(viewState: UploadingUIState, onCloseUploading: () -> Unit, onRetry: (uuid: String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize()) {
            CloseTopAppBar(
                title = stringResource(R.string.cloud_storage_upload_list_title),
                onClose = onCloseUploading,
            )
            UploadFileList(viewState.uploadFiles, onRetry)
        }
    }
}

@Composable
fun UploadFileList(uploadFiles: List<UploadFile>, onRetry: (uuid: String) -> Unit) {
    LazyColumn {
        items(
            count = uploadFiles.size,
            key = { index: Int ->
                uploadFiles[index].fileUUID
            }
        ) { index ->
            UploadFileItem(uploadFiles[index], onClick = { onRetry(uploadFiles[index].fileUUID) })
        }
    }
}

@Composable
private fun UploadFileItem(file: UploadFile, onClick: () -> Unit) {
    val imageId = when (file.filename.fileSuffix()) {
        "jpg", "jpeg", "png", "webp" -> R.drawable.ic_cloud_file_image
        "ppt", "pptx" -> R.drawable.ic_cloud_file_ppt
        "doc", "docx" -> R.drawable.ic_cloud_file_word
        "pdf" -> R.drawable.ic_cloud_file_pdf
        "mp4" -> R.drawable.ic_cloud_file_video
        "mp3", "aac" -> R.drawable.ic_cloud_file_audio
        else -> R.drawable.ic_cloud_file_others
    }
    val desc = when (file.uploadState) {
        UploadState.Failure -> stringResource(R.string.upload_failure)
        else -> "${FlatFormatter.size((file.size * file.progress).toLong())} / ${
            FlatFormatter.size(
                file.size
            )
        }"
    }

    val modifier = if (file.uploadState == UploadState.Failure) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(modifier) {
        Row(Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(16.dp))
            Image(painterResource(imageId), contentDescription = "", modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                FlatTextBodyOne(
                    file.filename,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = FlatTheme.colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                FlatTextCaption(desc)
            }
            Spacer(Modifier.width(12.dp))
            Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                when (file.uploadState) {
                    UploadState.Init, UploadState.Uploading -> {
                        FlatTextCaption("${(file.progress * 100).toInt()}%", color = FlatTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            FlatCircularProgressIndicator(progress = file.progress, Modifier.size(16.dp))
                        }
                    }
                    UploadState.Success -> {
                        FlatTextCaption("100%", color = FlatTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(painterResource(id = R.drawable.ic_cloud_upload_success), "", Modifier.size(24.dp))
                    }
                    UploadState.Failure -> {
                        FlatTextCaption(stringResource(id = R.string.retry), color = FlatTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(painterResource(id = R.drawable.ic_cloud_upload_failure), "", Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
        }
        FlatDivider(modifier = Modifier.align(Alignment.BottomCenter), startIndent = 52.dp, endIndent = 12.dp)
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun UploadFileItemPreview() {
    val failure = UploadFile("444", "444.jpg", uploadState = UploadState.Failure, progress = 0.6f)
    val uploading = UploadFile("444", "444.jpg", uploadState = UploadState.Uploading, progress = 0.6f)
    val success = UploadFile("444", "444.jpg", uploadState = UploadState.Success, progress = 1.0f)
    FlatPage {
        Column {
            UploadFileItem(failure) {}
            UploadFileItem(success) {}
            UploadFileItem(uploading) {}
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun UploadListPreview() {
    val uploadFiles = listOf(
        UploadFile("111", "11111111111111111111111111111111111111111111.jpg"),
        UploadFile("222", "222.jpg", uploadState = UploadState.Uploading, progress = 0.2f),
        UploadFile("333", "333.jpg", uploadState = UploadState.Success, progress = 1f),
        UploadFile("444", "444.jpg", uploadState = UploadState.Failure, progress = 0.6f),
    )
    val viewState = UploadingUIState(uploadFiles = uploadFiles)
    FlatPage {
        Uploading(viewState, {}, { uuid -> })
    }
}

