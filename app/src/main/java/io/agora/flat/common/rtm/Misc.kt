package io.agora.flat.common.rtm

import io.agora.rtm.*

internal interface EmptyRtmClientListener : RtmClientListener {
    override fun onConnectionStateChanged(state: Int, reason: Int) {

    }

    override fun onMessageReceived(message: RtmMessage, peerId: String) {

    }

    override fun onImageMessageReceivedFromPeer(message: RtmImageMessage, peerId: String) {

    }

    override fun onFileMessageReceivedFromPeer(message: RtmFileMessage, peerId: String) {

    }

    override fun onMediaUploadingProgress(message: RtmMediaOperationProgress, peerId: Long) {

    }

    override fun onMediaDownloadingProgress(progress: RtmMediaOperationProgress, requestId: Long) {

    }

    override fun onTokenExpired() {

    }

    override fun onPeersOnlineStatusChanged(peersStatus: MutableMap<String, Int>) {

    }
}

internal interface EmptyRtmChannelListener : RtmChannelListener {
    override fun onMemberCountUpdated(count: Int) {}

    override fun onAttributesUpdated(attributes: MutableList<RtmChannelAttribute>) {}

    override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {}

    override fun onImageMessageReceived(imageMessage: RtmImageMessage, member: RtmChannelMember) {}

    override fun onFileMessageReceived(fileMessage: RtmFileMessage, member: RtmChannelMember) {}

    override fun onMemberJoined(member: RtmChannelMember) {}

    override fun onMemberLeft(member: RtmChannelMember) {}
}

internal interface RTMListener {
    fun onClassEvent(event: ClassRtmEvent)

    fun onChatMessage(chatMessage: ChatMessage)

    fun onMemberJoined(userId: String, channelId: String)

    fun onMemberLeft(userId: String, channelId: String)

    fun onRemoteLogin() {}
}

internal object Codes {
    const val CONNECTION_STATE_DISCONNECTED = 1
    const val CONNECTION_STATE_CONNECTING = 2
    const val CONNECTION_STATE_CONNECTED = 3
    const val CONNECTION_STATE_RECONNECTING = 4
    const val CONNECTION_STATE_ABORTED = 5

    const val CONNECTION_CHANGE_REASON_LOGIN = 1
    const val CONNECTION_CHANGE_REASON_LOGIN_SUCCESS = 2
    const val CONNECTION_CHANGE_REASON_LOGIN_FAILURE = 3
    const val CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT = 4
    const val CONNECTION_CHANGE_REASON_INTERRUPTED = 5
    const val CONNECTION_CHANGE_REASON_LOGOUT = 6
    const val CONNECTION_CHANGE_REASON_BANNED_BY_SERVER = 7
    const val CONNECTION_CHANGE_REASON_REMOTE_LOGIN = 8
}