package io.agora.flat.ui.activity.bind

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
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
class EmailBindViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val bindingState = ObservableLoadingCounter()
    private val bindSuccess = MutableStateFlow(false)
    private val codeSuccess = MutableStateFlow(false)
    private val uiMessageManager = UiMessageManager()

    private var _state = MutableStateFlow(EmailBindUiState.Empty)
    val state: StateFlow<EmailBindUiState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                bindSuccess,
                codeSuccess,
                bindingState.observable,
                uiMessageManager.message
            ) { bindSuccess, codeSuccess, binding, message ->
                EmailBindUiState(
                    bindSuccess = bindSuccess,
                    codeSuccess = codeSuccess,
                    binding = binding,
                    message = message,
                )
            }.collect {
                _state.value = it
            }
        }
    }

    fun sendEmailCode(email: String) {
        viewModelScope.launch {
            userRepository.requestBindEmailCode(email = email)
                .onSuccess {
                    codeSuccess.value = true
                }
                .onFailure {
                    notifyMessage(UiErrorMessage(it))
                }
        }
    }

    fun bindEmail(email: String, code: String) {
        viewModelScope.launch {
            bindingState.addLoader()
            userRepository.bindEmail(email = email, code = code)
                .onSuccess {
                    eventBus.produceEvent(UserBindingsUpdated())
                    bindSuccess.value = true
                }.onFailure {
                    uiMessageManager.emitMessage(UiErrorMessage(it))
                }
            bindingState.removeLoader()
        }
    }

    private fun notifyMessage(message: UiMessage) {
        viewModelScope.launch {
            uiMessageManager.emitMessage(message)
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }

    fun clearCodeSuccess() {
        viewModelScope.launch {
            codeSuccess.value = false
        }
    }
}

data class EmailBindUiState(
    val bindSuccess: Boolean = false,
    val codeSuccess: Boolean = false,
    val binding: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Empty = EmailBindUiState()
    }
}