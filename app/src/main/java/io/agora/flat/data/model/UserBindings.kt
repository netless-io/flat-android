package io.agora.flat.data.model

data class UserBindings(
    val wechat: Boolean = false,
    val phone: Boolean = false,
    val email: Boolean = false,
    val agora: Boolean = false,
    val apple: Boolean = false,
    val github: Boolean = false,
    val google: Boolean = false,

    val meta: Meta,
) {
    fun bindingCount(): Int {
        return listOf(wechat, phone, email, agora, apple, github, google).count { it }
    }
}

data class Meta(
    val wechat: String = "",
    val phone: String = "",
    val apple: String = "",
    val agora: String = "",
    val github: String = "",
    val email: String = "",
    val google: String = "",
)