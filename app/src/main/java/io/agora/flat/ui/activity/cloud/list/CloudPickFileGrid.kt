package io.agora.flat.ui.activity.cloud.list

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.compose.FlatTextCaption
import io.agora.flat.ui.compose.launcherPickContent
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.util.ContentInfo

private data class PickItemData(
    @DrawableRes val id: Int,
    @StringRes val text: Int,
    val metadataType: String,
)

private val allItems = listOf(
    PickItemData(R.drawable.ic_upload_file_image, R.string.cloud_storage_upload_image, "image/*"),
    PickItemData(R.drawable.ic_upload_file_video, R.string.cloud_storage_upload_video, "video/*"),
    PickItemData(R.drawable.ic_upload_file_audio, R.string.cloud_storage_upload_music, "audio/*"),
    PickItemData(R.drawable.ic_upload_file_doc, R.string.cloud_storage_upload_doc, "*/*"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CloudPickFileGrid(onUploadFile: (uri: Uri, info: ContentInfo) -> Unit) {
    val launcher: (String) -> Unit = launcherPickContent {
        onUploadFile(it.uri, it)
    }
    val count = if (isTabletMode()) 4 else 2
    LazyVerticalGrid(columns = GridCells.Fixed(count)) {
        items(allItems.size) { index ->
            UploadPickItem(
                allItems[index].id,
                allItems[index].text,
                Modifier.height(PickFileItemHeight)
            ) {
                launcher(allItems[index].metadataType)
            }
        }
    }
}

@Composable
private fun UploadPickItem(@DrawableRes id: Int, @StringRes text: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier, Alignment.Center) {
        Column(
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 48.dp),
                onClick = onClick,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(id), contentDescription = "", Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            FlatTextCaption(stringResource(text), color = FlatTheme.colors.textPrimary)
        }
    }
}

internal val PickFileItemHeight = 125.dp