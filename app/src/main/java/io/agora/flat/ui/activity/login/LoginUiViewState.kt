package io.agora.flat.ui.activity.login

import io.agora.flat.common.login.LoginState
import io.agora.flat.data.model.UserInfo
import io.agora.flat.ui.util.UiError

data class LoginUiViewState(
    val user: UserInfo? = null,
    val loginState: LoginState = LoginState.Init,
    val uiError: UiError? = null,
) {
    companion object {
        val Empty = LoginUiViewState();
    }
}