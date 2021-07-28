package io.agora.flat.di.impl

import android.content.Context
import io.agora.flat.Constants
import io.agora.flat.common.RTCEventHandler
import io.agora.flat.common.RTCEventListener
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoEncoderConfiguration

class RtcProviderImpl : RtcEngineProvider, StartupInitializer {
    private lateinit var mRtcEngine: RtcEngine
    private val mHandler: RTCEventHandler = RTCEventHandler()

    override fun onCreate(context: Context) {
        try {
            mRtcEngine = RtcEngine.create(context, Constants.AGORA_APP_ID, mHandler)
            // mRtcEngine.setLogFile(FileUtil.initializeLogFile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupVideoConfig()
    }

    override fun onTerminate() {
        super.onTerminate()
        RtcEngine.destroy()
    }

    private fun setupVideoConfig() {
        mRtcEngine.enableVideo()
        mRtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
            )
        )
    }

    override fun rtcEngine(): RtcEngine {
        return mRtcEngine
    }

    override fun addEventListener(listener: RTCEventListener) {
        mHandler.addListener(listener)
    }

    override fun removeEventListener(listener: RTCEventListener) {
        mHandler.removeListener(listener)
    }
}