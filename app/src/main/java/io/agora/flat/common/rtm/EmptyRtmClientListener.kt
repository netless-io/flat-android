package io.agora.flat.common.rtm

import io.agora.rtm.*

interface EmptyRtmClientListener : RtmClientListener {
    override fun onConnectionStateChanged(p0: Int, p1: Int) {

    }

    override fun onMessageReceived(p0: RtmMessage, p1: String) {

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