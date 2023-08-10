package io.agora.flat.data.model

data class PhoneRegisterReq constructor(
    val phone: String,
    val code: String,
    // 8..32 length
    val password: String,
)
