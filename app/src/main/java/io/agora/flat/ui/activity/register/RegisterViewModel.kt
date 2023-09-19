package io.agora.flat.ui.activity.register

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.data.AppEnv
import io.agora.flat.data.model.PhoneOrEmailInfo
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.base.BaseAccountViewModel
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appEnv: AppEnv,
) : BaseAccountViewModel() {
    private val loading = ObservableLoadingCounter()

    private var _state = MutableStateFlow(
        RegisterUiState(
            info = PhoneOrEmailInfo(
                cc = CallingCodeManager.getDefaultCC(),
                phoneFirst = appEnv.phoneFirst
            )
        )
    )
    val state: StateFlow<RegisterUiState>
        get() = _state

    init {
        viewModelScope.launch {
            loading.observable.collect {
                _state.value = _state.value.copy(loading = it)
            }
        }

        viewModelScope.launch {
            remainTime.collect {
                val info = state.value.info.copy(remainTime = it)
                _state.value = _state.value.copy(info = info)
            }
        }
    }

    private fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.requestRegisterSmsCode(phone = phone)
                .onSuccess {
                    notifySendCodeSuccess()
                    startCountDown()
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
                    startCountDown()
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

    fun updateRegisterInfo(it: PhoneOrEmailInfo) {
        _state.value = _state.value.copy(info = it)
    }

    fun sendCode() {
        val info = state.value.info
        val sendPhoneCode = info.isPhone
        if (sendPhoneCode) {
            sendSmsCode(info.phone)
        } else {
            sendEmailCode(info.email)
        }
    }

    fun register() {
        val info = state.value.info
        val registerPhone = info.isPhone
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
        _state.value = _state.value.copy(error = UiErrorMessage(throwable))
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class RegisterUiState(
    val success: Boolean = false,
    val sendCodeSuccess: Boolean = false,
    val info: PhoneOrEmailInfo = PhoneOrEmailInfo(),
    val loading: Boolean = false,
    val error: UiMessage? = null
)