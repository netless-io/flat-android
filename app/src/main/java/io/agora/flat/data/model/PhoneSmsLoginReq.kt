package io.agora.flat.data.model

data class PhoneSmsLoginReq constructor(
    val phone: String,
    val code: String,
)
