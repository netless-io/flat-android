package io.agora.flat.common.login

enum class LoginType {
    None,
    WeChat,
    Github,
    Google,
}

sealed class LoginState {
    object Init : LoginState();
    data class Process(val message: String) : LoginState()
    object Success : LoginState()
}