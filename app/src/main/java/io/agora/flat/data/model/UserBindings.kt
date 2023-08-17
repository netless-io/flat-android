package io.agora.flat.data.model

data class UserBindings(
    val wechat: Boolean = false,
    val phone: Boolean = false,
    val email: Boolean = false,
    val agora: Boolean = false,
    val apple: Boolean = false,
    val github: Boolean = false,
    val google: Boolean = false,
)