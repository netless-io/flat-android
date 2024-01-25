package io.agora.flat.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import io.agora.flat.common.rtc.AgoraRtc
import io.agora.flat.data.AppKVCenter

val LocalAgoraRtc = staticCompositionLocalOf<AgoraRtc?> {
    null
}


val LocalAppKVCenter = staticCompositionLocalOf<AppKVCenter?> {
    null
}