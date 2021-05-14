package io.agora.flat.data.model

data class RtcUser(val name: String, val rtcUID: Int, val avatarURL: String) {
    var audioOpen = true
    var videoOpen = true
}
