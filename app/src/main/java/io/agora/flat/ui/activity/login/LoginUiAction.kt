package io.agora.flat.ui.activity.login


sealed class LoginUiAction {
    object WeChatLogin : LoginUiAction()
    object GithubLogin : LoginUiAction()

    object OpenServiceProtocol : LoginUiAction()
    object OpenPrivacyProtocol : LoginUiAction()

    data class PhoneSendCode(val phone: String) : LoginUiAction()
    data class PhoneLogin(val phone: String, val code: String) : LoginUiAction()

    object SignUpClick : LoginUiAction()

    class PasswordLoginClick(val state: LoginInputState) : LoginUiAction()
    class LoginInputChange(val state: LoginInputState) : LoginUiAction()

    object ForgotPwdClick : LoginUiAction()
}