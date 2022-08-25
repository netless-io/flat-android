package io.agora.flat.di.interfaces

import io.agora.flat.common.rtm.ClassRtmEvent
import io.agora.flat.common.rtm.RtmMember
import io.agora.flat.data.model.ORDER_ASC
import io.agora.flat.data.model.RtmQueryMessage
import kotlinx.coroutines.flow.Flow

interface RtmApi {
    suspend fun login(rtmToken: String, channelId: String, userUUID: String): Boolean

    suspend fun logout(): Boolean

    suspend fun getMembers(): List<RtmMember>

    suspend fun sendChannelMessage(msg: String): Boolean

    suspend fun sendChannelCommand(event: ClassRtmEvent): Boolean

    suspend fun sendPeerCommand(event: ClassRtmEvent, peerId: String): Boolean

    /**
     * 获取历史聊天消息
     */
    suspend fun getTextHistory(
        channelId: String,
        startTime: Long,
        endTime: Long,
        limit: Int = 100,
        offset: Int = 0,
        order: String = ORDER_ASC,
    ): List<RtmQueryMessage>

    suspend fun getTextHistoryCount(
        channelId: String,
        startTime: Long,
        endTime: Long,
    ): Int

    fun observeRtmEvent(): Flow<ClassRtmEvent>
}


