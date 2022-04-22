package io.agora.flat.ui.activity.{PACKAGE_NAME}

data class {ACTIVITY_NAME}UiViewState(
    val bindSuccess: Boolean = false,
    val binding: Boolean = false,
) {
    companion object {
        val Empty = {ACTIVITY_NAME}UiViewState()
    }
}