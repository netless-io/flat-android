package io.agora.flat.ui.activity.phone

data class PhoneBindUiViewState(
    val bindSuccess: Boolean = false,
    val binding: Boolean = false,
) {
    companion object {
        val Empty = PhoneBindUiViewState()
    }
}