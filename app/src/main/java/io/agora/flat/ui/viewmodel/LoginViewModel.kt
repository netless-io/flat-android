package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter
) : ViewModel() {
    init {

    }

    suspend fun loginSetAuthUUID(): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            val authUUID = UUID.randomUUID().toString()
            appKVCenter.setAuthUUID(authUUID)

            when (userRepository.loginSetAuthUUID(authUUID)) {
                is Success -> {
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resume(false)
                }
            }
        }
    }

    fun githubLoginUrl(): String {
        return "https://github.com/login/oauth/authorize?" +
                "client_id=${Constants.GITHUB_CLIENT_ID}&" +
                "redirect_uri=${Constants.GITHUB_CALLBACK}&" +
                "state=${appKVCenter.getAuthUUID()}"
    }

    suspend fun loginProcess(): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (userRepository.loginProcess(appKVCenter.getAuthUUID())) {
                is Success -> cont.resume(true)
                is ErrorResult -> cont.resume(false)
            }
        }
    }

    suspend fun loginWeChatCallback(code: String): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (val resp = userRepository.loginWeChatCallback(appKVCenter.getAuthUUID(), code)) {
                is Success -> cont.resume(true)
                is ErrorResult -> cont.resume(false)
            }
        }
    }
}