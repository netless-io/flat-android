package io.agora.flat.ui.activity.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatColorWhite
import io.agora.flat.ui.theme.FlatTitleTextStyle
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTopAppBar
import io.agora.flat.util.showDebugToast

@Composable
fun CloudStorage() {
    val context = LocalContext.current

    FlatColumnPage {
        FlatCloudStorageTopBar()
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = { context.showDebugToast(R.string.toast_in_development) },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = FlatColorWhite)
            }
        }
    }
}

@Composable
private fun FlatCloudStorageTopBar() {
    FlatTopAppBar(title = { Text(stringResource(id = R.string.title_cloud_storage), style = FlatTitleTextStyle) })
}

@Composable
@Preview
private fun CloudStoragePreview() {
    CloudStorage()
}