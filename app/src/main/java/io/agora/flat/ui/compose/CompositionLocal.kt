package io.agora.flat.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import io.agora.flat.common.rtc.AgoraRtc

val LocalAgoraRtc = staticCompositionLocalOf<AgoraRtc?> {
    null
}
