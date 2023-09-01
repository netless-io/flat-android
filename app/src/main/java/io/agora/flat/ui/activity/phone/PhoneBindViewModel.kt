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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBindViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : BaseAccountViewModel() {
    private val uiMessageManager = UiMessageManager()
    private val bindingState = ObservableLoadingCounter()
    private val bindSuccess = MutableStateFlow(false)
    private val codeSuccess = MutableStateFlow(false)


    private var _state = MutableStateFlow(PhoneBindUiViewState.Empty)
    val state: StateFlow<PhoneBindUiViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                bindSuccess,
                codeSuccess,
                remainTime,
                bindingState.observable,
                uiMessageManager.message
            ) { bindSuccess, codeSuccess, remainTime, binding, message ->
                PhoneBindUiViewState(
                    bindSuccess = bindSuccess,
                    codeSuccess = codeSuccess,
                    remainTime = remainTime,
                    binding = binding,
                    message = message,
                )
            }.collect {
                _state.value = it
            }
        }
    }

    fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            userRepository.requestBindSmsCode(phone = phone)
                .onSuccess {
                    codeSuccess.value = true
                }.onFailure {
                    uiMessageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }

    fun bindPhone(phone: String, code: String) {
        viewModelScope.launch {
            bindingState.addLoader()
            userRepository.bindPhone(phone = phone, code = code).onSuccess {
                eventBus.produceEvent(UserBindingsUpdated())
                bindSuccess.value = true
                startCountDown()
            }.onFailure {
                uiMessageManager.emitMessage(UiErrorMessage(it))
            }
            bindingState.removeLoader()
        }
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }

    fun clearCodeSuccess() {
        codeSuccess.value = false
    }
}

data class PhoneBindUiViewState(
    val bindSuccess: Boolean = false,
    val codeSuccess: Boolean = false,
    val remainTime: Long = 0L,
    val binding: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Empty = PhoneBindUiViewState()
    }
}