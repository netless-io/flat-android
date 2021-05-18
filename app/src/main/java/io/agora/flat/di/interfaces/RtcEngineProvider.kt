package io.agora.flat.di.interfaces

import io.agora.flat.common.RTCEventListener
import io.agora.rtc.RtcEngine

interface RtcEngineProvider {
    fun rtcEngine(): RtcEngine

    fun addEventListener(listener: RTCEventListener)

    fun removeEventListener(listener: RTCEventListener);
}