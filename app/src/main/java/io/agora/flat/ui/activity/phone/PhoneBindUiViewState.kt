package io.agora.flat.ui.activity.phone

import io.agora.flat.ui.util.UiMessage

data class PhoneBindUiViewState(
    val bindSuccess: Boolean = false,
    val binding: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Empty = PhoneBindUiViewState()
    }
}