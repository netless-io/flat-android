package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserUpdated
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditNameViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val messageManager = UiMessageManager()

    private val _state = MutableStateFlow(EditNameUiState(name = userRepository.getUsername()))
    val state: StateFlow<EditNameUiState>
        get() = _state

    init {
        viewModelScope.launch {
            messageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            userRepository.rename(name = name)
                .onSuccess {
                    eventBus.produceEvent(UserUpdated)
                    _state.value = EditNameUiState(name = name, success = true)
                }
                .onFailure {
                    messageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }
}

data class EditNameUiState(
    val name: String,
    val success: Boolean = false,
    val message: UiMessage? = null,
)