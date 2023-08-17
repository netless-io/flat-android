package io.agora.flat.ui.activity.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiInfoMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val loading = ObservableLoadingCounter()
    private val uiMessageManager = UiMessageManager()

    private var _state = MutableStateFlow(PasswordChangeUiState.Init)
    val state: StateFlow<PasswordChangeUiState>
        get() = _state

    init {
        viewModelScope.launch {
            loading.observable.collect {
                _state.value = _state.value.copy(loading = it)
            }
        }

        viewModelScope.launch {
            uiMessageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            loading.addLoader()
            userRepository.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _state.value = _state.value.copy(success = true)
                }
                .onFailure {
                    showUiError(it)
                }
            loading.removeLoader()
        }
    }

    private fun showUiInfo(message: String) {
        viewModelScope.launch {
            uiMessageManager.emitMessage(UiInfoMessage(message))
        }
    }

    private fun showUiError(e: Throwable) {
        viewModelScope.launch {
            uiMessageManager.emitMessage(UiErrorMessage(e))
        }
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class PasswordChangeUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Init = PasswordChangeUiState()
    }
}