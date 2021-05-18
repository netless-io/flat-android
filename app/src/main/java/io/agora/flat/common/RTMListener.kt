package io.agora.flat.common

import io.agora.flat.data.model.RTMEvent

interface RTMListener {
    fun onRTMEvent(rtmEvent: RTMEvent, senderId: String)
    fun onMemberJoined(userId: String, channelId: String)
    fun onMemberLeft(userId: String, channelId: String)
}