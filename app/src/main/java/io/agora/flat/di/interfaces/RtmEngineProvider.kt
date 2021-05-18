package io.agora.flat.di.interfaces

import io.agora.flat.common.FlatRTMListener
import io.agora.flat.data.model.RTMEvent
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmClient

interface RtmEngineProvider {
    fun rtmEngine(): RtmClient

    suspend fun initChannel(rtmToken: String, channelId: String, userUUID: String): Boolean

    suspend fun getMembers(): List<RtmChannelMember>

    suspend fun sendChannelMessage(msg: String): Boolean

    suspend fun sendChannelCommand(event: RTMEvent): Boolean

    suspend fun sendPeerCommand(event: RTMEvent, peerId: String): Boolean

    fun addFlatRTMListener(listener: FlatRTMListener)

    fun removeFlatRTMListener(listener: FlatRTMListener)
}