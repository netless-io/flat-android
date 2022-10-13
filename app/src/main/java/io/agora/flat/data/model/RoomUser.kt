package io.agora.flat.data.model

data class RoomUser(
    val userUUID: String,
    val rtcUID: Int = NOT_JOIN_RTC_UID,
    val name: String? = null,
    val avatarURL: String = "",

    val isSpeak: Boolean = false,
    val audioOpen: Boolean = false,
    val videoOpen: Boolean = false,
    val isRaiseHand: Boolean = false,

    val isOwner: Boolean = false,
) {
    val isOnStage: Boolean
        get() = rtcUID > 0 && (isSpeak || isOwner)

    val allowDraw: Boolean
        get() = isOwner || isSpeak

    val isJoined: Boolean
        get() = rtcUID > 0

    val isLeft: Boolean
        get() = !isJoined

    companion object {
        const val NOT_JOIN_RTC_UID = 0
    }
}
