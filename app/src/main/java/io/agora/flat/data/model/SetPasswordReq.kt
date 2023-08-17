package io.agora.flat.data.model

data class SetPasswordReq constructor(
    val password: String? = null,
    val newPassword: String,
)
