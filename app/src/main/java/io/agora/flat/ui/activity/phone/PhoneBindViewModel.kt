package io.agora.flat.ui.activity.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBindViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    private val bindingState = ObservableLoadingCounter()
    private val bindSuccess = MutableStateFlow(false)

    private var _state = MutableStateFlow(PhoneBindUiViewState.Empty)
    val state: StateFlow<PhoneBindUiViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(bindSuccess, bindingState.observable) { bindSuccess, binding ->
                PhoneBindUiViewState(
                    bindSuccess = bindSuccess,
                    binding = binding
                )
            }.collect {
                _state.value = it;
            }
        }
    }

    fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            val sendResult = userRepository.requestBindSmsCode(phone = phone);
            if (sendResult is Success) {
                showUiMessage(stringFetcher.loginCodeSend())
            } else {
                when ((sendResult as Failure).error.code) {
                    FlatErrorCode.Web_SMSAlreadyExist -> {
                        showUiMessage(stringFetcher.phoneBound())
                    }
                    FlatErrorCode.Web_SMSAlreadyBinding -> {
                        showUiMessage(stringFetcher.phoneBound())
                    }
                    else -> {
                        showUiMessage(stringFetcher.commonFail())
                    }
                }
            }
        }
    }

    fun bindPhone(phone: String, code: String) {
        viewModelScope.launch {
            bindingState.addLoader()
            val bindResult = userRepository.bindPhone(phone = phone, code = code)
            bindingState.removeLoader()
            if (bindResult is Success) {
                notifyBindSuccess()
            } else {
                when ((bindResult as Failure).error.code) {
                    FlatErrorCode.Web_SMSAlreadyExist -> {
                        showUiMessage(stringFetcher.alreadyHasPhone())
                    }
                    FlatErrorCode.Web_SMSVerificationCodeInvalid -> {
                        showUiMessage(stringFetcher.invalidVerificationCode())
                    }
                    FlatErrorCode.Web_ExhaustiveAttack -> {
                        showUiMessage(stringFetcher.frequentRequest())
                    }
                    else -> {
                        showUiMessage(stringFetcher.commonFail())
                    }
                }
            }
        }
    }

    private fun showUiMessage(message: String) {
        _state.value = _state.value.copy(message = UiMessage(message))
    }

    private fun notifyBindSuccess() {
        _state.value = _state.value.copy(bindSuccess = true, message = null)
    }
}