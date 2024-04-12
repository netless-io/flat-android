package io.agora.flat.common.rtc

import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler

internal class RTCEventHandler : IRtcEngineEventHandler() {
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

    override fun onRejoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onRejoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onLeaveChannel(stats: RtcStats) {
        for (listener in listeners) {
            listener.onLeaveChannel(stats)
        }
    }

    override fun onConnectionStateChanged(state: Int, reason: Int) {
        super.onConnectionStateChanged(state, reason)
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

    override fun onLocalVideoStats(source: Constants.VideoSourceType, stats: LocalVideoStats) {
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

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>, totalVolume: Int) {
        for (listener in listeners) {
            listener.onAudioVolumeIndication(speakers, totalVolume)
        }
    }

    override fun onPermissionError(permission: Int) {
        for (listener in listeners) {
            listener.onPermissionError(permission)
        }
    }

    override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onRemoteAudioStateChanged(uid, state, reason, elapsed)
        }
    }

    override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
        for (listener in listeners) {
            listener.onRemoteVideoStateChanged(uid, state, reason, elapsed)
        }
    }

    override fun onUserMuteAudio(uid: Int, muted: Boolean) {
        for (listener in listeners) {
            listener.onUserMuteAudio(uid, muted)
        }
    }

    override fun onUserMuteVideo(uid: Int, muted: Boolean) {
        for (listener in listeners) {
            listener.onUserMuteVideo(uid, muted)
        }
    }
}

internal interface RTCEventListener {
    fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {}

    fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {}

    fun onRejoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {}

    fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?) {}

    fun onUserOffline(uid: Int, reason: Int) {}

    fun onUserJoined(uid: Int, elapsed: Int) {}

    fun onLastmileQuality(quality: Int) {}

    fun onLastmileProbeResult(result: IRtcEngineEventHandler.LastmileProbeResult) {}

    fun onLocalVideoStats(stats: IRtcEngineEventHandler.LocalVideoStats) {}

    fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats) {}

    fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {}

    fun onRemoteVideoStats(stats: IRtcEngineEventHandler.RemoteVideoStats) {}

    fun onRemoteAudioStats(stats: IRtcEngineEventHandler.RemoteAudioStats) {}

    fun onAudioVolumeIndication(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>, totalVolume: Int) {}
    fun onPermissionError(permission: Int) {}

    fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {}

    fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {}

    fun onUserMuteAudio(uid: Int, muted: Boolean) {}

    fun onUserMuteVideo(uid: Int, muted: Boolean) {}
}