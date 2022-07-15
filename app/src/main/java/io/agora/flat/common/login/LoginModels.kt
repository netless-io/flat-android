package io.agora.flat.common.login

import io.agora.flat.ui.util.UiMessage

enum class LoginType {
    None,
    WeChat,
    Github,
    Google,
}

sealed class LoginState {
    object Init : LoginState()
    data class Process(val message: UiMessage) : LoginState()
    object Success : LoginState()
}