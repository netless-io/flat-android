package io.agora.flat.di.interfaces

import io.agora.flat.common.EventHandler
import io.agora.rtc.RtcEngine

interface RtcEngineProvider {
    fun rtcEngine(): RtcEngine

    fun registerEventHandler(handler: EventHandler)

    fun removeEventHandler(handler: EventHandler);
}