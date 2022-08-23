package io.agora.flat.common.rtc

data class RtcJoinOptions(
    val token: String,
    val channel: String,
    val uid: Int,

    val audioOpen: Boolean = false,
    val videoOpen: Boolean = false,
)
