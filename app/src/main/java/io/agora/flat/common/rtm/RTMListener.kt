package io.agora.flat.common.rtm

import io.agora.flat.data.model.RTMEvent

interface RTMListener {
    fun onRTMEvent(event: RTMEvent, senderId: String)

    fun onMemberJoined(userId: String, channelId: String)

    fun onMemberLeft(userId: String, channelId: String)
}