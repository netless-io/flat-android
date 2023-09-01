package io.agora.flat.ui.activity.login

import io.agora.flat.data.model.PhoneOrEmailInfo


sealed class LoginUiAction {
    object WeChatLogin : LoginUiAction()
    object GithubLogin : LoginUiAction()
    object GoogleLogin : LoginUiAction()

    object OpenServiceProtocol : LoginUiAction()
    object OpenPrivacyProtocol : LoginUiAction()

    data class PhoneSendCode(val phone: String) : LoginUiAction()
    data class PhoneLogin(val phone: String, val code: String) : LoginUiAction()

    object SignUpClick : LoginUiAction()

    class PasswordLoginClick(val info: PhoneOrEmailInfo) : LoginUiAction()
    class LoginInputChange(val info: PhoneOrEmailInfo) : LoginUiAction()

    object ForgotPwdClick : LoginUiAction()
}