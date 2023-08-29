package io.agora.flat.common.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
import io.agora.flat.ui.activity.setting.AccountSecurityActivity
import io.agora.flat.util.showToast
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * bindingHandler -> thirdParty(webview, wechat) -> XXXEntryActivity -> LoginManager -> bindingHandler.handleResult
 */
@ActivityScoped
class UserBindingHandler @Inject constructor(
    @ActivityContext val context: Context,
    private val loginManager: LoginManager,
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    private val appEnv: AppEnv,
    private val eventBus: EventBus,
) {
    private val scope = (context as AppCompatActivity).lifecycleScope
    private var loginType: LoginType = LoginType.None

    fun bindWithType(type: LoginType) {
        loginType = type
        loginManager.actionClazz = AccountSecurityActivity::class.java
        scope.launch {
            when (loginType) {
                LoginType.WeChat -> callWeChatLogin()
                LoginType.Github -> callWebViewBinding(::getGithubBindingUrl)
                LoginType.Google -> callWebViewBinding(::getGoogleBindingUrl)
                else -> {}
            }
        }
    }

    private suspend fun bindingSetAuthUUID(): Boolean {
        val authUUID = UUID.randomUUID().toString()
        appKVCenter.setAuthUUID(authUUID)
        return userRepository.bindingSetAuthUUID(authUUID) is Success
    }

    private suspend fun callWeChatLogin() {
        if (bindingSetAuthUUID()) {
            loginManager.callWeChatAuth()
        }
    }

    private suspend fun callWebViewBinding(getUrl: () -> String) {
        if (bindingSetAuthUUID()) {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(getUrl())
            }

            val chooserIntent = Intent.createChooser(
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

    private fun getGithubBindingUrl(): String {
        val redirectUri = "${appEnv.githubBindingCallback}?platform=android&state=${appKVCenter.getAuthUUID()}"
        return "https://github.com/login/oauth/authorize?" +
                "client_id=${appEnv.githubClientID}&" +
                "redirect_uri=${Uri.encode(redirectUri)}"
    }

    private fun getGoogleBindingUrl(): String {
        val scopes = listOf("openid", "https://www.googleapis.com/auth/userinfo.profile")
        val scope = Uri.encode(scopes.joinToString(" "))
        val redirectUri = appEnv.googleBindingCallback

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "response_type=code" +
                "&access_type=online" +
                "&scope=$scope" +
                "&client_id=${appEnv.googleClientId}" +
                "&redirect_uri=$redirectUri" +
                "&state=${appKVCenter.getAuthUUID()}"
    }

    private suspend fun bindingProcess(): Boolean {
        return userRepository.bindingProcess(appKVCenter.getAuthUUID()) is Success
    }

    private suspend fun bindWeChat(code: String): Boolean {
        return userRepository.bindWeChat(appKVCenter.getAuthUUID(), code) is Success
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
                        showUiMessage(context.getString(R.string.bind_fail))
                        return@launch
                    }

                    val code = intent.getStringExtra(Constants.Login.KEY_LOGIN_RESP) ?: ""
                    if (bindWeChat(code)) {
                        notifySuccess()
                    } else {
                        showUiMessage(context.getString(R.string.bind_fail))
                    }
                }

                LoginType.Github -> {
                    if (intent.data?.scheme != "x-agora-flat-client") {
                        return@launch
                    }
                    if (bindingProcess()) {
                        notifySuccess()
                    } else {
                        showUiMessage(context.getString(R.string.bind_fail))
                    }
                }

                LoginType.Google -> {
                    if (intent.data?.scheme != "x-agora-flat-client") {
                        return@launch
                    }
                    if (bindingProcess()) {
                        notifySuccess()
                    } else {
                        showUiMessage(context.getString(R.string.bind_fail))
                    }
                }

                else -> {
                }
            }
        }
    }

    private fun showUiMessage(string: String) {
        context.showToast(string)
    }

    private fun notifySuccess() {
        context.showToast(R.string.bind_success)
        loginType = LoginType.None
        scope.launch {
            eventBus.produceEvent(UserBindingsUpdated())
        }
    }
}