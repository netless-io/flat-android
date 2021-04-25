package io.agora.flat.di.interfaces

import io.agora.rtm.RtmClient

interface RtmEngineProvider {
    fun rtmEngine(): RtmClient
}