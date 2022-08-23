package io.agora.flat.data.model

// a class for record user in room. may be named as ClassUser / RoomUser etc.
data class RtcUser(
    val userUUID: String,
    val rtcUID: Int = NOT_JOIN_RTC_UID,
    val name: String? = null,
    val avatarURL: String = "",

    val isSpeak: Boolean = false,
    val audioOpen: Boolean = false,
    val videoOpen: Boolean = false,
    val isRaiseHand: Boolean = false,

    val isOwner: Boolean = false,
    val isSelf: Boolean = false
) {

    companion object {
        const val NOT_JOIN_RTC_UID = 0
    }

    val isNotJoin: Boolean
        get() = rtcUID == NOT_JOIN_RTC_UID
}
