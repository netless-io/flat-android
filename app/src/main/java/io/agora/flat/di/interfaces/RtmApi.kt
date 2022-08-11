package io.agora.flat.di.interfaces

import android.util.Log
import io.agora.flat.common.rtm.ClassRtmEvent
import io.agora.flat.data.model.ORDER_ASC
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.data.model.RtmQueryMessage
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmClient
import kotlinx.coroutines.flow.Flow

interface RtmApi {
    fun rtmEngine(): RtmClient

    suspend fun login(rtmToken: String, channelId: String, userUUID: String): Boolean

    suspend fun logout(): Boolean

    suspend fun getMembers(): List<RtmChannelMember>

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

    fun observeClassEvent(): Flow<ClassRtmEvent>
}


