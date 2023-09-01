package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Failure
import io.agora.flat.data.LoginConfig
import io.agora.flat.data.Success
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
    appEnv: AppEnv,
) : ViewModel() {
    private val messageManager = UiMessageManager()
    private val userBindings = MutableStateFlow<UserBindings?>(null)
    private val userInfo = MutableStateFlow(userRepository.getUserInfo())

    private val _state = MutableStateFlow(AccountSecurityUiState(loginConfig = appEnv.loginConfig))
    val state: StateFlow<AccountSecurityUiState>
        get() = _state

    init {
        viewModelScope.launch {
            userRepository.validateDeleteAccount()
                .onSuccess {
                    _state.value = _state.value.copy(roomCount = it.count)
                }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<UserBindingsUpdated>().collect {
                userBindings.value = userRepository.getBindings()
            }
        }

        viewModelScope.launch {
            combine(userInfo, userBindings, messageManager.message) { userInfo, bindings, message ->
                val current = _state.value
                current.copy(
                    userInfo = userInfo,
                    bindings = bindings,
                    message = message,
                )
            }.collect {
                _state.value = it
            }
        }

        loadBindings()
    }

    private fun loadBindings() {
        viewModelScope.launch {
            userBindings.value = userRepository.listBindings().get()
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            userInfo.value = userRepository.getUserInfo()
        }
    }

    fun processUnbind(platform: LoginPlatform) {
        viewModelScope.launch {
            userRepository.removeBinding(platform).onFailure {
                messageManager.emitMessage(UiErrorMessage(it))
            }
            userBindings.value = userRepository.getBindings()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            when (val result = userRepository.deleteAccount()) {
                is Success -> {
                    userRepository.logout()
                    _state.value = _state.value.copy(deleteAccount = true)
                }

                is Failure -> messageManager.emitMessage(UiMessage("delete account fail", result.exception))
            }
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }
}

data class AccountSecurityUiState(
    val roomCount: Int = 0,
    val loading: Boolean = false,
    val deleteAccount: Boolean = false,
    val message: UiMessage? = null,

    val userInfo: UserInfo? = null,
    val bindings: UserBindings? = null,

    val loginConfig: LoginConfig = LoginConfig(),
) {
    val bindingCount
        get() = bindings?.run {
            listOf(wechat, phone, email, agora, apple, github, google).count { it }
        } ?: 0
}