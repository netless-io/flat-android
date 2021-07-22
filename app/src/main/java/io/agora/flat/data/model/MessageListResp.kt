package io.agora.flat.data.model

data class MessageListResp(
    val result: String,
    val code: String,
    val messages: List<RtmQueryMessage>,
    val request_id: String,
)
