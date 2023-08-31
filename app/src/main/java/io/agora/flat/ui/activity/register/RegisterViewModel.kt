package io.agora.flat.ui.activity.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.util.isValidPhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val loading = ObservableLoadingCounter()

    private var _state = MutableStateFlow(RegisterUiState.Init)
    val state: StateFlow<RegisterUiState>
        get() = _state

    init {
        viewModelScope.launch {
            loading.observable.collect {
                _state.value = _state.value.copy(loading = it)
            }
        }
    }

    private fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.requestRegisterSmsCode(phone = phone)
                .onSuccess {
                    notifySendCodeSuccess()
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    private fun registerPhone(phone: String, code: String, password: String) {
        viewModelScope.launch {

            userRepository.registerWithPhone(phone = phone, code = code, password = password)
                .onSuccess {
                    _state.value = _state.value.copy(success = true)
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    private fun sendEmailCode(email: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.requestRegisterEmailCode(email = email)
                .onSuccess {
                    notifySendCodeSuccess()
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    private fun registerEmail(email: String, code: String, password: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.registerWithEmail(email = email, code = code, password = password)
                .onSuccess {
                    _state.value = _state.value.copy(success = true)
                }
                .onFailure {
                    notifyError(it)
                }
            loading.removeLoader()
        }
    }

    fun updateRegisterInfo(it: RegisterInfo) {
        _state.value = _state.value.copy(registerInfo = it)
    }

    fun sendCode() {
        val info = state.value.registerInfo
        val sendPhoneCode = info.phoneMode
        if (sendPhoneCode) {
            sendSmsCode(info.phone)
        } else {
            sendEmailCode(info.email)
        }
    }

    fun register() {
        val info = state.value.registerInfo
        val registerPhone = info.phoneMode
        if (registerPhone) {
            registerPhone(info.phone, info.code, info.password)
        } else {
            registerEmail(info.email, info.code, info.password)
        }
    }

    private fun notifySendCodeSuccess() {
        _state.value = _state.value.copy(sendCodeSuccess = true)
    }

    fun clearSendCodeState() {
        _state.value = _state.value.copy(sendCodeSuccess = false)
    }

    private fun notifyError(throwable: Throwable) {
        _state.value = _state.value.copy(error = UiMessage(throwable.message ?: "", throwable))
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}


data class RegisterInfo(
    val value: String = "",
    val cc: String = "",
    val code: String = "",
    val password: String = "",
) {
    val phone: String
        get() = "$cc$value"

    val email: String
        get() = value

    val phoneMode: Boolean
        get() = value.isValidPhone()
}

data class RegisterUiState(
    val success: Boolean = false,
    val sendCodeSuccess: Boolean = false,
    val registerInfo: RegisterInfo = RegisterInfo(
        cc = CallingCodeManager.getDefaultCC(),
    ),
    val loading: Boolean = false,
    val error: UiMessage? = null
) {
    companion object {
        val Init = RegisterUiState()
    }
}