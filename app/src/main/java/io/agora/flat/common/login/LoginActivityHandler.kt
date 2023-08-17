package io.agora.flat.common.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.error.FlatErrorHandler
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class LoginActivityHandler(
    val context: Context,
    private val loginManager: LoginManager,
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    private val appEnv: AppEnv,
) {
    private val scope = (context as AppCompatActivity).lifecycleScope
    private val _state = MutableStateFlow<LoginState>(LoginState.Init)
    private var loginType: LoginType = LoginType.None

    fun loginWithType(type: LoginType) {
        _state.value = LoginState.Init
        loginType = type
        loginManager.actionClazz = LoginActivity::class.java
        scope.launch {
            when (loginType) {
                LoginType.WeChat -> callWeChatLogin()
                LoginType.Github -> callGithubLogin()
                else -> {}
            }
        }
    }

    private suspend fun loginSetAuthUUID(): Boolean {
        val authUUID = UUID.randomUUID().toString()
        appKVCenter.setAuthUUID(authUUID)
        return userRepository.loginSetAuthUUID(authUUID) is Success
    }

    private suspend fun callWeChatLogin() {
        if (loginSetAuthUUID()) {
            loginManager.callWeChatAuth()
        }
    }

    private suspend fun callGithubLogin() {
        // ensure authUUID the same as githubLoginUrl
        if (loginSetAuthUUID()) {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(githubLoginUrl())
            }

            val chooserIntent = createChooser(
                intent,
                context.getString(R.string.intent_browser_choose_title)
            )

            try {
                context.startActivity(chooserIntent)
            } catch (e: ActivityNotFoundException) {
                showUiMessage(context.getString(R.string.intent_no_browser))
            }
        }
    }

    private fun githubLoginUrl(): String {
        val redirectUri = "${appEnv.githubCallback}?platform=android&state=${appKVCenter.getAuthUUID()}"
        return "https://github.com/login/oauth/authorize?" +
                "client_id=${appEnv.githubClientID}&" +
                "redirect_uri=${Uri.encode(redirectUri)}"
    }

    private suspend fun loginProcess(): Boolean {
        return userRepository.loginProcess(appKVCenter.getAuthUUID()) is Success
    }

    private suspend fun loginWeChatCallback(code: String): Boolean {
        return userRepository.loginWeChatCallback(appKVCenter.getAuthUUID(), code) is Success
    }

    fun handleResult(intent: Intent) {
        scope.launch {
            when (loginType) {
                LoginType.WeChat -> {
                    if (!intent.hasExtra(Constants.Login.KEY_LOGIN_STATE)) {
                        return@launch
                    }
                    val result = intent.getIntExtra(Constants.Login.KEY_LOGIN_STATE, Constants.Login.AUTH_ERROR)
                    if (result != Constants.Login.AUTH_SUCCESS) {
                        showUiMessage(context.getString(R.string.login_fail))
                        return@launch
                    }

                    val code = intent.getStringExtra(Constants.Login.KEY_LOGIN_RESP) ?: ""
                    if (loginWeChatCallback(code)) {
                        notifySuccess()
                    } else {
                        showUiMessage(context.getString(R.string.login_fail))
                    }
                }
                LoginType.Github -> {
                    if (intent.data?.scheme != "x-agora-flat-client") {
                        return@launch
                    }
                    if (loginProcess()) {
                        notifySuccess()
                    } else {
                        showUiMessage(context.getString(R.string.login_fail))
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun showUiMessage(string: String) {
        _state.value = LoginState.Process(UiMessage(string))
    }

    private fun notifySuccess() {
        loginType = LoginType.None
        _state.value = LoginState.Success
    }

    fun observeLoginState(): StateFlow<LoginState> {
        return _state.asStateFlow()
    }

    fun sendPhoneCode(phone: String) {
        scope.launch {
            val sendResult = userRepository.requestLoginSmsCode(phone)
            if (sendResult is Success) {
                showUiMessage(context.getString(R.string.login_code_send))
            } else {
                showUiMessage(context.getString(R.string.error_request_common_fail))
            }
        }
    }

    fun loginWithPhone(phone: String, code: String) {
        scope.launch {
            when (val loginResult = userRepository.loginWithPhone(phone, code)) {
                is Success -> notifySuccess()
                is Failure -> {
                    showUiMessage(
                        FlatErrorHandler.getErrorStr(
                            context,
                            loginResult.exception,
                            context.getString(R.string.error_request_common_fail)
                        )
                    )
                }
            }
        }
    }
}