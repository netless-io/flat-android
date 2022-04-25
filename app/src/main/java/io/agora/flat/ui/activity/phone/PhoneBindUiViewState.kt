package io.agora.flat.ui.activity.phone

data class PhoneBindUiViewState(
    val bindSuccess: Boolean = false,
    val binding: Boolean = false,
    val message: String? = null,
) {
    companion object {
        val Empty = PhoneBindUiViewState()
    }
}