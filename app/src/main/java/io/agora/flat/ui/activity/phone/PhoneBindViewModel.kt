package io.agora.flat.ui.activity.phone

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
import io.agora.flat.ui.activity.base.BaseAccountViewModel
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBindViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : BaseAccountViewModel() {
    private val messageManager = UiMessageManager()
    private val loadingCounter = ObservableLoadingCounter()
    private val success = MutableStateFlow(false)
    private val codeSuccess = MutableStateFlow(false)
    private val mergingState = MutableStateFlow(false)

    private var _state = MutableStateFlow(PhoneBindUiViewState.Empty)
    val state: StateFlow<PhoneBindUiViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                success,
                codeSuccess,
                remainTime,
                mergingState,
                loadingCounter.observable,
                messageManager.message
            ) { bindSuccess, codeSuccess, remainTime, merging, loading, message ->
                PhoneBindUiViewState(
                    success = bindSuccess,
                    codeSuccess = codeSuccess,
                    remainTime = remainTime,
                    merging = merging,
                    loading = loading,
                    message = message,
                )
            }.collect {
                _state.value = it
            }
        }
    }

    private fun sendBindSmsCode(phone: String) {
        viewModelScope.launch {
            userRepository.requestBindSmsCode(phone = phone)
                .onSuccess {
                    codeSuccess.value = true
                    startCountDown()
                }.onFailure {
                    messageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }

    private fun bindPhone(phone: String, code: String) {
        viewModelScope.launch {
            loadingCounter.addLoader()
            userRepository.bindPhone(phone = phone, code = code).onSuccess {
                eventBus.produceEvent(UserBindingsUpdated())
                success.value = true
            }.onFailure {
                messageManager.emitMessage(UiErrorMessage(it))
            }
            loadingCounter.removeLoader()
        }
    }

    private fun sendMergeSmsCode(phone: String) {
        viewModelScope.launch {
            userRepository.requestRebindPhoneCode(phone = phone)
                .onSuccess {
                    codeSuccess.value = true
                    startCountDown()
                }.onFailure {
                    messageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }

    private fun mergeByPhone(phone: String, code: String) {
        viewModelScope.launch {
            loadingCounter.addLoader()
            userRepository.rebindWithPhone(phone = phone, code = code).onSuccess {
                success.value = true
            }.onFailure {
                messageManager.emitMessage(UiErrorMessage(it))
            }
            loadingCounter.removeLoader()
        }
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }

    fun clearCodeSuccess() {
        codeSuccess.value = false
    }

    fun setMerging(merging: Boolean) {
        mergingState.value = merging
    }

    fun sendCode(phone: String) {
        if (state.value.merging) {
            sendMergeSmsCode(phone)
        } else {
            sendBindSmsCode(phone)
        }
    }

    fun confirmBind(phone: String, code: String) {
        if (state.value.merging) {
            mergeByPhone(phone, code)
        } else {
            bindPhone(phone, code)
        }
    }
}

data class PhoneBindUiViewState(
    val success: Boolean = false,
    val codeSuccess: Boolean = false,
    val remainTime: Long = 0L,
    val merging: Boolean = false,
    val loading: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Empty = PhoneBindUiViewState()
    }
}