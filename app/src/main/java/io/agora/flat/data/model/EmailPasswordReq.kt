package io.agora.flat.data.model

data class EmailPasswordReq(
    val email: String,
    // 8..32 length
    val password: String,
)
