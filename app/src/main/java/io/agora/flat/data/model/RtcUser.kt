package io.agora.flat.data.model

data class RtcUser(
    var name: String = "",
    var rtcUID: Int = NOT_JOIN_RTC_UID,
    var avatarURL: String = "",

    var userUUID: String = "",
    var audioOpen: Boolean = false,
    var videoOpen: Boolean = false,
    var isSpeak: Boolean = false,
    var isRaiseHand: Boolean = false,
) {

    companion object {
        const val NOT_JOIN_RTC_UID = 0;
    }

    val isNotJoin: Boolean
        get() {
            return rtcUID == NOT_JOIN_RTC_UID
        }
}
