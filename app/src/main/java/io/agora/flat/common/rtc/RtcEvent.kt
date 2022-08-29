package io.agora.flat.common.rtc

sealed class RtcEvent {
    data class UserOffline(val uid: Int, val reason: Int) : RtcEvent()

    data class UserJoined(val uid: Int, val elapsed: Int) : RtcEvent()
}