package io.agora.flat.data.model

import io.agora.flat.util.isValidEmail
import io.agora.flat.util.isValidPhone

data class PhoneOrEmailInfo(
    val value: String = "",
    val cc: String = "",
    val code: String = "",
    val password: String = "",
    val remainTime: Long = 0,
    val phoneFirst: Boolean = true,
) {
    val phone: String
        get() = "$cc$value"

    val email: String
        get() = value

    val isPhone: Boolean
        get() = value.isValidPhone()

    val isValidPhoneOrEmail: Boolean
        get() = value.isValidPhone() || value.isValidEmail()
}
