package io.agora.flat.common

import io.agora.rtc.IRtcEngineEventHandler.*

interface RTCEventListener {
    fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int)
    fun onLeaveChannel(stats: RtcStats?)
    fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onUserJoined(uid: Int, elapsed: Int)
    fun onLastmileQuality(quality: Int) {}
    fun onLastmileProbeResult(result: LastmileProbeResult?) {}
    fun onLocalVideoStats(stats: LocalVideoStats?) {}
    fun onRtcStats(stats: RtcStats?) {}
    fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {}
    fun onRemoteVideoStats(stats: RemoteVideoStats?) {}
    fun onRemoteAudioStats(stats: RemoteAudioStats?) {}
}