package io.agora.flat.ui.activity.{PACKAGE_NAME}

sealed class {ACTIVITY_NAME}UiAction {
    object Close : {ACTIVITY_NAME}UiAction()
    data class Bind(val phone: String, val code: String) : {ACTIVITY_NAME}UiAction()
}