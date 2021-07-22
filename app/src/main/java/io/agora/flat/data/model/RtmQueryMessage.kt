package io.agora.flat.data.model

data class RtmQueryMessage(
    val dst: String,
    val message_type: String,
    val ms: Long,
    val payload: String,
    val src: String,
)