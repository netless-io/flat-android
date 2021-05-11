package io.agora.flat.di.impl

import android.content.Context
import android.util.Log
import io.agora.flat.Constants
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtm.*

class RtmProviderImpl : RtmEngineProvider, StartupInitializer {

    companion object {
        val TAG = RtmProviderImpl::class.simpleName
    }

    private lateinit var mRtmClient: RtmClient

    override fun onCreate(context: Context) {
        try {
            mRtmClient = RtmClient.createInstance(
                context,
                Constants.AGORA_APP_ID,
                object : RtmClientListener {
                    override fun onConnectionStateChanged(state: Int, reason: Int) {
                        Log.d(TAG, "Connection state changes to $state reason:$reason")
                    }

                    override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {
                        Log.d(TAG, "Message received  from $peerId ${rtmMessage.text}")
                    }

                    override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {
                    }

                    override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {
                    }

                    override fun onMediaUploadingProgress(
                        p0: RtmMediaOperationProgress,
                        p1: Long
                    ) {
                    }

                    override fun onMediaDownloadingProgress(
                        p0: RtmMediaOperationProgress,
                        p1: Long
                    ) {
                    }

                    override fun onTokenExpired() {
                    }

                    override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
                    }
                })
        } catch (e: java.lang.Exception) {
            Log.d(TAG, "RTM SDK init fatal error!")
            throw RuntimeException("You need to check the RTM init process.")
        }
    }

    override fun rtmEngine(): RtmClient {
        return mRtmClient
    }
}