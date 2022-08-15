package io.agora.flat.common.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { callback.value(it) }
    }
}
