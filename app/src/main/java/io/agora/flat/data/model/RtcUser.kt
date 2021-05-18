package io.agora.flat.data.model

data class RtcUser(
    var name: String, var rtcUID: Int, var avatarURL: String,

    var userUUID: String = "",
    var audioOpen: Boolean = true,
    var videoOpen: Boolean = true,
    var isSpeak: Boolean = false,
    var isRaiseHand: Boolean = false,
) {
}
