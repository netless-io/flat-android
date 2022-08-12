package io.agora.flat.data.model

data class RtcUser(
    var userUUID: String,
    var name: String? = null,
    var avatarURL: String = "",

    var rtcUID: Int = NOT_FETCHED,

    var isSpeak: Boolean = false,
    var audioOpen: Boolean = false,
    var videoOpen: Boolean = false,
    var isRaiseHand: Boolean = false,
) {

    companion object {
        const val NOT_FETCHED = -1
        const val NOT_JOIN_RTC_UID = 0
    }

    val isNotJoin: Boolean
        get() {
            return rtcUID == NOT_JOIN_RTC_UID
        }
}
