package io.agora.flat

import android.app.Application
import com.tencent.mm.opensdk.utils.Log
import dagger.hilt.android.HiltAndroidApp
import io.agora.flat.common.AgoraEventHandler
import io.agora.flat.common.EventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rtm.*

@HiltAndroidApp
class MainApplication : Application() {
    private lateinit var mRtmClient: RtmClient
    private lateinit var mRtcEngine: RtcEngine
    private val mHandler: AgoraEventHandler = AgoraEventHandler()

    companion object {
        val TAG = "MainApplication"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            mRtcEngine = RtcEngine.create(applicationContext, Constants.AGORA_APP_ID, mHandler)
            // mRtcEngine.setLogFile(FileUtil.initializeLogFile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupVideoConfig()

        try {
            mRtmClient = RtmClient.createInstance(
                applicationContext,
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
                        p0: RtmMediaOperationProgress?,
                        p1: Long
                    ) {
                    }

                    override fun onMediaDownloadingProgress(
                        p0: RtmMediaOperationProgress?,
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

    private fun setupVideoConfig() {
        mRtcEngine.enableVideo()
        mRtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }


    fun rtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    fun rtmClient(): RtmClient? {
        return mRtmClient
    }

    fun registerEventHandler(handler: EventHandler) {
        mHandler.addHandler(handler)
    }

    fun removeEventHandler(handler: EventHandler) {
        mHandler.removeHandler(handler)
    }

    override fun onTerminate() {
        super.onTerminate()
        RtcEngine.destroy()
    }
}