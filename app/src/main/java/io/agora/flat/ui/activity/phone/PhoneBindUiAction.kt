package io.agora.flat.ui.activity.phone

sealed class PhoneBindUiAction {
    object Close : PhoneBindUiAction()

    data class SendCode(val phone: String) : PhoneBindUiAction()

    data class Bind(val phone: String, val code: String) : PhoneBindUiAction()
}