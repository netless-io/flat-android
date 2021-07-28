package io.agora.flat.di.interfaces

import io.agora.flat.common.RTMListener
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.data.model.RtmQueryMessage
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmClient

interface RtmEngineProvider {
    fun rtmEngine(): RtmClient

    suspend fun initChannel(rtmToken: String, channelId: String, userUUID: String): Boolean

    suspend fun getMembers(): List<RtmChannelMember>

    suspend fun sendChannelMessage(msg: String): Boolean

    suspend fun sendChannelCommand(event: RTMEvent): Boolean

    suspend fun sendPeerCommand(event: RTMEvent, peerId: String): Boolean

    /**
     * 获取历史聊天消息
     */
    suspend fun getTextHistory(
        channelId: String,
        startTime: Long,
        endTime: Long,
        limit: Int = 100,
    ): List<RtmQueryMessage>

    fun addFlatRTMListener(listener: RTMListener)

    fun removeFlatRTMListener(listener: RTMListener)
}