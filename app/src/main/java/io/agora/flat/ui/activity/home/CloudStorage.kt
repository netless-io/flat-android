package io.agora.flat.ui.activity.home

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.R
import io.agora.flat.ui.activity.ui.theme.FlatTitleTextStyle
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTopAppBar

@Composable
fun CloudStorage() {
    FlatColumnPage {
        FlatCloudStorageTopBar()
    }
}

@Preview
@Composable
private fun FlatCloudStorageTopBar() {
    FlatTopAppBar(
        title = {
            Text(stringResource(id = R.string.title_cloud_storage), style = FlatTitleTextStyle)
        }
    )
}