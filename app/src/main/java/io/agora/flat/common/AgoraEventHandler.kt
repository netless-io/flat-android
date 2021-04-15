package io.agora.flat.common

import io.agora.rtc.IRtcEngineEventHandler
import java.util.*

class AgoraEventHandler : IRtcEngineEventHandler() {
    private val mHandler = ArrayList<EventHandler>()

    fun addHandler(handler: EventHandler) {
        mHandler.add(handler)
    }

    fun removeHandler(handler: EventHandler) {
        mHandler.remove(handler)
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        for (handler in mHandler) {
            handler.onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onLeaveChannel(stats: RtcStats) {
        for (handler in mHandler) {
            handler.onLeaveChannel(stats)
        }
    }

    override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
        for (handler in mHandler) {
            handler.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
        }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        for (handler in mHandler) {
            handler.onUserJoined(uid, elapsed)
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        for (handler in mHandler) {
            handler.onUserOffline(uid, reason)
        }
    }

    override fun onLocalVideoStats(stats: LocalVideoStats) {
        for (handler in mHandler) {
            handler.onLocalVideoStats(stats)
        }
    }

    override fun onRtcStats(stats: RtcStats) {
        for (handler in mHandler) {
            handler.onRtcStats(stats)
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        for (handler in mHandler) {
            handler.onNetworkQuality(uid, txQuality, rxQuality)
        }
    }

    override fun onRemoteVideoStats(stats: RemoteVideoStats) {
        for (handler in mHandler) {
            handler.onRemoteVideoStats(stats)
        }
    }

    override fun onRemoteAudioStats(stats: RemoteAudioStats) {
        for (handler in mHandler) {
            handler.onRemoteAudioStats(stats)
        }
    }

    override fun onLastmileQuality(quality: Int) {
        for (handler in mHandler) {
            handler.onLastmileQuality(quality)
        }
    }

    override fun onLastmileProbeResult(result: LastmileProbeResult) {
        for (handler in mHandler) {
            handler.onLastmileProbeResult(result)
        }
    }
}