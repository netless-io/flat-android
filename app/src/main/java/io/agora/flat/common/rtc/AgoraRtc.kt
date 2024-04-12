package io.agora.flat.common.rtc

import android.content.Context
import io.agora.flat.data.AppEnv
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraRtc @Inject constructor(val appEnv: AppEnv, val logger: Logger) : RtcApi, StartupInitializer {
    private lateinit var rtcEngine: RtcEngine
    private val eventHandler: RTCEventHandler = RTCEventHandler()
    private var currentUid = 0

    override fun init(context: Context) {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = appEnv.agoraAppId
                mEventHandler = eventHandler
            }
            rtcEngine = RtcEngine.create(config)
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
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
    }

    fun rtcEngine(): RtcEngine {
        return rtcEngine
    }

    override fun joinChannel(options: RtcJoinOptions): Int {
        currentUid = options.uid
        logger.i("[RTC] create media options by state: video=${options.videoOpen}, audio=${options.audioOpen}")
        val mediaOptions = ChannelMediaOptions().apply {
            clientRoleType =
                if (options.videoOpen || options.audioOpen) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishCameraTrack = options.videoOpen
            publishMicrophoneTrack = options.audioOpen
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        return rtcEngine.joinChannel(options.token, options.channel, options.uid, mediaOptions)
    }

    override fun leaveChannel(): Int {
        currentUid = 0
        return rtcEngine.leaveChannel()
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
        val mediaOptions = ChannelMediaOptions().apply {
            clientRoleType = if (audio || video) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishCameraTrack = video
            publishMicrophoneTrack = audio
        }
        rtcEngine.updateChannelMediaOptions(mediaOptions)
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
                speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>, totalVolume: Int
            ) {
                val info = speakers.map {
                    AudioVolumeInfo(uid = it.uid, volume = it.volume, vad = it.vad)
                }
                trySend(RtcEvent.VolumeIndication(info, totalVolume))
            }

            override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                if (uid == currentUid) {
                    trySend(RtcEvent.NetworkStatus(getOverallQuality(txQuality, rxQuality)))
                }
            }

            override fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats) {
                trySend(RtcEvent.LastmileDelay(stats.lastmileDelay))
            }

            override fun onPermissionError(permission: Int) {
                logger.e("[RTC] permission error: $permission")
            }

            override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                logger.i("[RTC] remote audio state changed: $uid, $state, $reason, $elapsed")
            }

            override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                logger.i("[RTC] remote video state changed: $uid, $state, $reason, $elapsed")
            }

            override fun onUserMuteAudio(uid: Int, muted: Boolean) {
                logger.i("[RTC] user mute audio: $uid, $muted")
            }

            override fun onUserMuteVideo(uid: Int, muted: Boolean) {
                logger.i("[RTC] user mute video: $uid, $muted")
            }
        }
        eventHandler.addListener(listener)
        awaitClose {
            logger.i("[RTC] rtc event flow closed")
            eventHandler.removeListener(listener)
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