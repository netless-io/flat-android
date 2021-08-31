package io.agora.flat.common.rtc

import io.agora.rtc.IRtcEngineEventHandler

class RTCEventHandler : IRtcEngineEventHandler() {
    private val listeners = ArrayList<RTCEventListener>()

    fun addListener(listener: RTCEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: RTCEventListener) {
        listeners.remove(listener)
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onLeaveChannel(stats: RtcStats) {
        for (listener in listeners) {
            listener.onLeaveChannel(stats)
        }
    }

    override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
        }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onUserJoined(uid, elapsed)
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        for (listener in listeners) {
            listener.onUserOffline(uid, reason)
        }
    }

    override fun onLocalVideoStats(stats: LocalVideoStats) {
        for (listener in listeners) {
            listener.onLocalVideoStats(stats)
        }
    }

    override fun onRtcStats(stats: RtcStats) {
        for (listener in listeners) {
            listener.onRtcStats(stats)
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        for (listener in listeners) {
            listener.onNetworkQuality(uid, txQuality, rxQuality)
        }
    }

    override fun onRemoteVideoStats(stats: RemoteVideoStats) {
        for (listener in listeners) {
            listener.onRemoteVideoStats(stats)
        }
    }

    override fun onRemoteAudioStats(stats: RemoteAudioStats) {
        for (listener in listeners) {
            listener.onRemoteAudioStats(stats)
        }
    }

    override fun onLastmileQuality(quality: Int) {
        for (listener in listeners) {
            listener.onLastmileQuality(quality)
        }
    }

    override fun onLastmileProbeResult(result: LastmileProbeResult) {
        for (listener in listeners) {
            listener.onLastmileProbeResult(result)
        }
    }
}