package io.agora.flat.di.impl

import android.content.Context
import io.agora.flat.Constants
import io.agora.flat.common.rtc.RTCEventHandler
import io.agora.flat.common.rtc.RTCEventListener
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoEncoderConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RtcProviderImpl @Inject constructor() : RtcEngineProvider, StartupInitializer {
    private lateinit var mRtcEngine: RtcEngine
    private val mHandler: RTCEventHandler = RTCEventHandler()

    override fun init(context: Context) {
        try {
            mRtcEngine = RtcEngine.create(context, Constants.AGORA_APP_ID, mHandler)
            // mRtcEngine.setLogFile(FileUtil.initializeLogFile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupVideoConfig()
    }

    private fun setupVideoConfig() {
        mRtcEngine.enableVideo()
        mRtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x480,
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