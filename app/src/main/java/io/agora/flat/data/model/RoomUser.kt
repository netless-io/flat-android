package io.agora.flat.data.model

data class RoomUser(
    val userUUID: String,
    val rtcUID: Int = NOT_JOIN_RTC_UID,
    val name: String? = null,
    val avatarURL: String = "",

    /**
     * 上台标识，Owner 用户该标记为 true
     */
    val isOnStage: Boolean = false,
    val audioOpen: Boolean = false,
    val videoOpen: Boolean = false,
    /**
     * 举手中标识
     */
    val isRaiseHand: Boolean = false,

    val isOwner: Boolean = false,
    /**
     * 白板权限标识，Owner 用户该标记为 true
     */
    val allowDraw: Boolean = false,
) {
    val isJoined: Boolean
        get() = rtcUID > 0

    companion object {
        const val NOT_JOIN_RTC_UID = 0
    }
}
