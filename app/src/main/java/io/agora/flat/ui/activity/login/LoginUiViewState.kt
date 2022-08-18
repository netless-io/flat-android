package io.agora.flat.ui.activity.login

import io.agora.flat.common.login.LoginState
import io.agora.flat.data.model.UserInfo

data class LoginUiViewState(
    val user: UserInfo? = null,
    val loginState: LoginState = LoginState.Init,
) {
    companion object {
        val Empty = LoginUiViewState()
    }
}