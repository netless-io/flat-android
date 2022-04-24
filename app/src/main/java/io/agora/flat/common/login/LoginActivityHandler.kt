package io.agora.flat.common.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.util.Ticker
import io.agora.flat.util.resolveActivity
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
        loginType = type
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
            loginManager.callWeChatLogin()
        }
    }

    private suspend fun callGithubLogin() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(githubLoginUrl())
        }

        if (context.resolveActivity(intent)) {
            if (loginSetAuthUUID()) {
                context.startActivity(Intent.createChooser(
                    intent,
                    context.getString(R.string.login_github_browser_choose_title),
                ))
            }
        } else {
            notifyUiError(context.getString(R.string.login_github_no_browser))
        }
    }

    private fun githubLoginUrl(): String {
        return "https://github.com/login/oauth/authorize?" +
                "client_id=${appEnv.githubClientID}&" +
                "redirect_uri=${appEnv.githubCallback}&" +
                "state=${appKVCenter.getAuthUUID()}"
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
                        notifyUiError(context.getString(R.string.login_fail))
                        return@launch
                    }

                    val code = intent.getStringExtra(Constants.Login.KEY_LOGIN_RESP) ?: ""
                    if (loginWeChatCallback(code)) {
                        notifySuccess()
                    } else {
                        notifyUiError(context.getString(R.string.login_fail))
                    }
                }
                LoginType.Github -> {
                    if (intent.data?.scheme != "x-agora-flat-client") {
                        return@launch
                    }
                    if (loginProcess()) {
                        notifySuccess()
                    } else {
                        notifyUiError(context.getString(R.string.login_fail))
                    }
                }
                else -> {}
            }
        }
    }

    private fun notifyUiError(string: String) {
        _state.value = LoginState.Process(string)
    }

    private fun notifySuccess() {
        _state.value = LoginState.Success
    }

    fun observeLoginState(): StateFlow<LoginState> {
        return _state.asStateFlow()
    }

    fun sendPhoneCode(phone: String) {
        scope.launch {
            // userRepository.requestLoginSmsCode(phone)
            Ticker.tickerFlow(1000)
        }
    }

    fun loginWithPhone(phone: String, code: String) {
        scope.launch {
            // userRepository.loginWithPhone(phone, code)
        }
    }
}