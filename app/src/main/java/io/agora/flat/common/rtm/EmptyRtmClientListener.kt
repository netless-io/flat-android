package io.agora.flat.common.rtm

import io.agora.rtm.*

interface EmptyRtmClientListener : RtmClientListener {
    override fun onConnectionStateChanged(state: Int, reason: Int) {

    }

    override fun onMessageReceived(message: RtmMessage, peerId: String) {

    }

    override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage, p1: String) {

    }

    override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage, p1: String) {

    }

    override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress, p1: Long) {

    }

    override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress, p1: Long) {

    }

    override fun onTokenExpired() {

    }

    override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>) {

    }
}