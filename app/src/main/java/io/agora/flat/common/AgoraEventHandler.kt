package io.agora.flat.common

import io.agora.rtc.IRtcEngineEventHandler

class AgoraEventHandler : IRtcEngineEventHandler() {
    private val handlers = ArrayList<EventHandler>()

    fun addHandler(handler: EventHandler) {
        handlers.add(handler)
    }

    fun removeHandler(handler: EventHandler) {
        handlers.remove(handler)
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        for (handler in handlers) {
            handler.onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onLeaveChannel(stats: RtcStats) {
        for (handler in handlers) {
            handler.onLeaveChannel(stats)
        }
    }

    override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
        for (handler in handlers) {
            handler.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
        }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        for (handler in handlers) {
            handler.onUserJoined(uid, elapsed)
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        for (handler in handlers) {
            handler.onUserOffline(uid, reason)
        }
    }

    override fun onLocalVideoStats(stats: LocalVideoStats) {
        for (handler in handlers) {
            handler.onLocalVideoStats(stats)
        }
    }

    override fun onRtcStats(stats: RtcStats) {
        for (handler in handlers) {
            handler.onRtcStats(stats)
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        for (handler in handlers) {
            handler.onNetworkQuality(uid, txQuality, rxQuality)
        }
    }

    override fun onRemoteVideoStats(stats: RemoteVideoStats) {
        for (handler in handlers) {
            handler.onRemoteVideoStats(stats)
        }
    }

    override fun onRemoteAudioStats(stats: RemoteAudioStats) {
        for (handler in handlers) {
            handler.onRemoteAudioStats(stats)
        }
    }

    override fun onLastmileQuality(quality: Int) {
        for (handler in handlers) {
            handler.onLastmileQuality(quality)
        }
    }

    override fun onLastmileProbeResult(result: LastmileProbeResult) {
        for (handler in handlers) {
            handler.onLastmileProbeResult(result)
        }
    }
}