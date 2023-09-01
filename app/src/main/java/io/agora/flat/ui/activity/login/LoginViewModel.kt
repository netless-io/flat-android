package io.agora.flat.ui.activity.login

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.base.BaseAccountViewModel
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.isValidPhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    private val appEnv: AppEnv,
) : BaseAccountViewModel() {
    private val loading = ObservableLoadingCounter()
    private val messageManager = UiMessageManager()

    private var _state = MutableStateFlow(LoginUiState.Init)
    val state: StateFlow<LoginUiState>
        get() = _state

    init {
        viewModelScope.launch {
            loading.observable.collect {
                _state.value = _state.value.copy(loading = it)
            }
        }

        viewModelScope.launch {
            messageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }

        viewModelScope.launch {
            remainTime.collect {
                val loginInputState = state.value.loginInputState.copy(remainTime = it)
                _state.value = _state.value.copy(loginInputState = loginInputState)
            }
        }

        viewModelScope.launch {
            appKVCenter.getLastLoginHistoryItem()?.let {
                val inputState = state.value.loginInputState
                val value = if (it.value.startsWith(inputState.cc)) {
                    it.value.substring(inputState.cc.length)
                } else {
                    it.value
                }
                _state.value = _state.value.copy(
                    loginInputState = inputState.copy(
                        value = value,
                        password = it.password
                    )
                )
            }
        }
    }

    fun needBindPhone(): Boolean {
        val bound = userRepository.getUserInfo()?.hasPhone ?: false
        return !bound && appEnv.loginConfig.forceBindPhone()
    }

    private fun loginEmail(email: String, password: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.loginWithEmailPassword(email = email, password = password)
                .onSuccess {
                    _state.value = _state.value.copy(success = true)
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    private fun loginPhone(phone: String, password: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.loginWithPhonePassword(phone = phone, password = password)
                .onSuccess {
                    _state.value = _state.value.copy(success = true)
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    fun login(state: LoginInputState) {
        val loginPhone = state.phoneMode
        if (loginPhone) {
            loginPhone(state.phone, state.password)
        } else {
            loginEmail(state.email, state.password)
        }
    }

    fun sendPhoneCode(phone: String) {
        viewModelScope.launch {
            userRepository.requestLoginSmsCode(phone)
                .onSuccess {
                    _state.value = _state.value.copy(sendCodeSuccess = true)
                    startCountDown()
                }.onFailure {
                    messageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }


    fun updateLoginInput(state: LoginInputState) {
        _state.value = _state.value.copy(loginInputState = state)
    }

    private fun notifyError(it: Throwable) {
        viewModelScope.launch {
            messageManager.emitMessage(UiMessage(it.message ?: "Unknown Error", it))
        }
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }

    fun clearSendCodeSuccess() {
        _state.value = _state.value.copy(sendCodeSuccess = false)
    }
}

@Parcelize
data class LoginInputState(
    // phone or email
    val value: String = "",
    val password: String = "",
    val smsCode: String = "",
    val remainTime: Long = 0,
    val cc: String = CallingCodeManager.getDefaultCC(),
) : Parcelable {
    val phone: String
        get() = "$cc$value"

    val email: String
        get() = value

    val phoneMode: Boolean
        get() = value.isValidPhone()
}

data class LoginUiState(
    val success: Boolean = false,
    val sendCodeSuccess: Boolean = false,
    val loginInputState: LoginInputState = LoginInputState(),
    val loading: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Init = LoginUiState()
    }
}