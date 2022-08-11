package io.agora.flat.di.impl

import android.content.Context
import io.agora.flat.common.rtc.RTCEventHandler
import io.agora.flat.common.rtc.RTCEventListener
import io.agora.flat.data.AppEnv
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RtcApiImpl @Inject constructor(val appEnv: AppEnv) : RtcApi, StartupInitializer {
    private lateinit var rtcEngine: RtcEngine
    private val mHandler: RTCEventHandler = RTCEventHandler()

    override fun init(context: Context) {
        try {
            rtcEngine = RtcEngine.create(context, appEnv.agoraAppId, mHandler)
            // rtcEngine.setLogFile(FileUtil.initializeLogFile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupVideoConfig()
    }

    private fun setupVideoConfig() {
        rtcEngine.enableVideo()
        rtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x480,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
            )
        )
        rtcEngine.adjustRecordingSignalVolume(200)
    }

    override fun rtcEngine(): RtcEngine {
        return rtcEngine
    }

    override fun joinChannel(token: String, channelName: String, optionalUid: Int): Int {
        return rtcEngine.joinChannel(token, channelName, "{}", optionalUid)
    }

    override fun leaveChannel() {
        rtcEngine.leaveChannel()
    }

    override fun setupLocalVideo(local: VideoCanvas) {
        rtcEngine.setupLocalVideo(local)
    }

    override fun setupRemoteVideo(remote: VideoCanvas) {
        rtcEngine.setupRemoteVideo(remote)
    }

    override fun updateLocalStream(audio: Boolean, video: Boolean) {
        // 使用 enableLocalAudio 关闭或开启本地采集后，本地听远端播放会有短暂中断。
        rtcEngine.enableLocalAudio(audio)
        rtcEngine.enableLocalVideo(video)
    }

    override fun updateRemoteStream(rtcUid: Int, audio: Boolean, video: Boolean) {
        rtcEngine.muteRemoteAudioStream(rtcUid, !audio)
        rtcEngine.muteRemoteVideoStream(rtcUid, !video)
    }

    override fun addEventListener(listener: RTCEventListener) {
        mHandler.addListener(listener)
    }

    override fun removeEventListener(listener: RTCEventListener) {
        mHandler.removeListener(listener)
    }
}