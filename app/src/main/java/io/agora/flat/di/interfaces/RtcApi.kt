package io.agora.flat.di.interfaces

import io.agora.flat.common.rtc.RTCEventListener
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

interface RtcApi {
    // Reserved for interface collection
    fun rtcEngine(): RtcEngine

    fun joinChannel(token: String, channelName: String, optionalUid: Int): Int

    fun leaveChannel()

    fun setupLocalVideo(local: VideoCanvas)

    fun setupRemoteVideo(remote: VideoCanvas)

    /**
     * enable local audio or video
     */
    fun updateLocalStream(audio: Boolean, video: Boolean)

    fun updateRemoteStream(rtcUid: Int, audio: Boolean, video: Boolean)

    fun addEventListener(listener: RTCEventListener)

    fun removeEventListener(listener: RTCEventListener)
}