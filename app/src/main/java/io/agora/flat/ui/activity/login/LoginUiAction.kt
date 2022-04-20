package io.agora.flat.ui.activity.login

sealed class LoginUiAction {
    object WeChatLogin : LoginUiAction()
    object GithubLogin : LoginUiAction()
    object AgreementHint : LoginUiAction()

    object OpenServiceProtocol : LoginUiAction()
    object OpenPrivacyProtocol : LoginUiAction()
}