package io.agora.flat.common.rtc

import android.content.Context
import io.agora.flat.data.AppEnv
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraRtc @Inject constructor(val appEnv: AppEnv, val logger: Logger) : RtcApi, StartupInitializer {
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

    fun rtcEngine(): RtcEngine {
        return rtcEngine
    }

    override fun joinChannel(options: RtcJoinOptions): Int {
        val channelMediaOptions = ChannelMediaOptions()
        channelMediaOptions.publishLocalVideo = options.videoOpen
        channelMediaOptions.publishLocalAudio = options.audioOpen
        return rtcEngine.joinChannel(options.token, options.channel, "{}", options.uid, channelMediaOptions)
    }

    override fun leaveChannel() {
        rtcEngine.leaveChannel()
    }

    override fun enableLocalVideo(enabled: Boolean) {
        rtcEngine.enableLocalVideo(enabled)
    }

    override fun enableLocalAudio(enabled: Boolean) {
        rtcEngine.enableLocalAudio(enabled)
    }

    override fun setupLocalVideo(local: VideoCanvas) {
        rtcEngine.setupLocalVideo(local)
    }

    override fun setupRemoteVideo(remote: VideoCanvas) {
        rtcEngine.setupRemoteVideo(remote)
    }

    override fun updateLocalStream(audio: Boolean, video: Boolean) {
        // 使用 enableLocalAudio 关闭或开启本地采集后，本地听远端播放会有短暂中断。
        rtcEngine.muteLocalAudioStream(!audio)
        rtcEngine.muteLocalVideoStream(!video)
    }

    override fun updateRemoteStream(rtcUid: Int, audio: Boolean, video: Boolean) {
        rtcEngine.muteRemoteAudioStream(rtcUid, !audio)
        rtcEngine.muteRemoteVideoStream(rtcUid, !video)
    }

    override fun observeRtcEvent(): Flow<RtcEvent> = callbackFlow {
        val listener = object : RTCEventListener {
            override fun onUserOffline(uid: Int, reason: Int) {
                trySend(RtcEvent.UserOffline(uid, reason))
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                trySend(RtcEvent.UserJoined(uid, elapsed))
            }

            override fun onAudioVolumeIndication(
                speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>,
                totalVolume: Int
            ) {
                val info = speakers.map {
                    AudioVolumeInfo(uid = it.uid, volume = it.volume, vad = it.vad)
                }
                trySend(RtcEvent.VolumeIndication(info, totalVolume))
            }

            override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                if (uid == 0) {
                    trySend(RtcEvent.NetworkStatus(getOverallQuality(txQuality, rxQuality)))
                }
            }

            override fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats) {
                trySend(RtcEvent.LastmileDelay(stats.lastmileDelay))
            }
        }
        mHandler.addListener(listener)
        awaitClose {
            logger.i("[RTC] rtc event flow closed")
            mHandler.removeListener(listener)
        }
    }

    companion object {
        internal fun getOverallQuality(txQuality: Int, rxQuality: Int): NetworkQuality {
            return when (maxOf(txQuality, rxQuality)) {
                Constants.QUALITY_UNKNOWN -> NetworkQuality.Unknown
                Constants.QUALITY_EXCELLENT -> NetworkQuality.Excellent
                Constants.QUALITY_GOOD -> NetworkQuality.Good
                else -> NetworkQuality.Bad
            }
        }
    }
}