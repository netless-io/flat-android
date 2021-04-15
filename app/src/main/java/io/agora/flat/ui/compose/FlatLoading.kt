package io.agora.flat.ui.compose

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import io.agora.flat.ui.activity.ui.theme.FlatColorBlue

@Composable
fun FlatPageLoading() {
    CircularProgressIndicator(color = FlatColorBlue)
}
