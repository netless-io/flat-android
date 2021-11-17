package io.agora.flat.ui.activity.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.R
import io.agora.flat.common.upload.UploadFile
import io.agora.flat.common.upload.UploadState
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.theme.*

@Composable
internal fun UploadList(onCloseUploading: () -> Unit, viewModel: UploadingViewModel = hiltViewModel()) {
    val viewState by viewModel.state.collectAsState()

    UploadList(viewState) { action ->
        when (action) {
            is UploadingUIAction.UploadRetry -> viewModel.retryUpload(action.fileUUID)
            is UploadingUIAction.UploadDelete -> viewModel.deleteUpload(action.fileUUID)
            is UploadingUIAction.CloseUploading -> onCloseUploading()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun UploadList(viewState: UploadingUIState, actioner: (UploadingUIAction) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize()) {
            CloseTopAppBar(
                title = stringResource(R.string.cloud_storage_upload_list_title),
                onClose = { actioner(UploadingUIAction.CloseUploading) },
            )
            UploadListContent(viewState.uploadFiles, actioner)
        }
    }
}

@Composable
fun UploadListContent(uploadFiles: List<UploadFile>, actioner: (UploadingUIAction) -> Unit) {
    LazyColumn {
        items(
            count = uploadFiles.size,
            key = { index: Int ->
                uploadFiles[index].fileUUID
            }
        ) { index ->
            UploadListItem(uploadFiles[index], actioner)
        }
    }
}

@Composable
private fun UploadListItem(uploadFile: UploadFile, actioner: (UploadingUIAction) -> Unit) {
    val context = LocalContext.current
    val info = when (uploadFile.uploadState) {
        UploadState.Success -> context.getString(R.string.upload_success)
        UploadState.Failure -> context.getString(R.string.upload_failure)
        else -> "${(uploadFile.progress * 100).toInt()}%"
    }

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
                uploadFile.filename,
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
            if (UploadState.Failure == uploadFile.uploadState) {
                Image(painterResource(R.drawable.ic_upload_retry), "",
                    Modifier.clickable { actioner(UploadingUIAction.UploadRetry(uploadFile.fileUUID)) })
                Spacer(Modifier.width(4.dp))
            }
            when (uploadFile.uploadState) {
                UploadState.Success -> Image(painterResource(R.drawable.ic_upload_success), contentDescription = "")
                else -> Image(painterResource(R.drawable.ic_upload_cancel), "",
                    Modifier.clickable { actioner(UploadingUIAction.UploadDelete(uploadFile.fileUUID)) })
            }
        }
        LinearProgressIndicator(
            progress = uploadFile.progress,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .align(Alignment.BottomCenter),
            color = progressColor
        )
    }
}

@Composable
@Preview
internal fun UploadListPreview() {
    val uploadFiles = listOf(
        UploadFile("111", "11111111111111111111111111111111111111111111.jpg"),
        UploadFile("222", "222.jpg", UploadState.Uploading, 0.2f),
        UploadFile("333", "333.jpg", UploadState.Success, 1f),
        UploadFile("444", "444.jpg", UploadState.Failure, progress = 0.6f),
    )
    val viewState = UploadingUIState(uploadFiles = uploadFiles)
    FlatPage {
        UploadList(viewState) {

        }
    }
}

@Composable
@Preview(heightDp = 100)
internal fun UploadListItemPreview() {
    val file = UploadFile("444", "444.jpg", UploadState.Failure, progress = 0.6f)
    UploadListItem(file) {

    }
}