package io.agora.flat.di.interfaces

import io.agora.flat.common.rtc.RTCEventListener
import io.agora.rtc.RtcEngine

interface RtcEngineProvider {
    // TODO
    fun rtcEngine(): RtcEngine

    fun addEventListener(listener: RTCEventListener)

    fun removeEventListener(listener: RTCEventListener)
}