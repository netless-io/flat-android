package io.agora.flat.common.rtm

interface RTMListener {
    fun onClassEvent(event: ClassRtmEvent)

    fun onChatMessage(chatMessage: ChatMessage)

    fun onMemberJoined(userId: String, channelId: String)

    fun onMemberLeft(userId: String, channelId: String)

    fun onRemoteLogin() {}
}