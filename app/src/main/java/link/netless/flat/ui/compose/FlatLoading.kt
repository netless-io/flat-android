package link.netless.flat.ui.compose

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import link.netless.flat.ui.activity.ui.theme.FlatColorBlue

@Composable
fun FlatPageLoading() {
    CircularProgressIndicator(color = FlatColorBlue)
}
