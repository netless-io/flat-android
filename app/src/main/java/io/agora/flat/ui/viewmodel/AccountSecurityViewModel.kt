package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val uiMessageManager = UiMessageManager()

    private val _state = MutableStateFlow(AccountSecurityUiState.Empty)
    val state: StateFlow<AccountSecurityUiState>
        get() = _state

    init {
        viewModelScope.launch {
            val result = userRepository.validateDeleteAccount()
            if (result is Success) {
                _state.value = _state.value.copy(roomCount = result.data.count)
            }
        }

        viewModelScope.launch {
            uiMessageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            when (val result = userRepository.deleteAccount()) {
                is Success -> {
                    userRepository.logout()
                    _state.value = _state.value.copy(deleteAccount = true)
                }
                is Failure -> uiMessageManager.emitMessage(UiMessage("delete account fail", result.exception))
            }
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class AccountSecurityUiState(
    val roomCount: Int = 0,
    val loading: Boolean = false,
    val deleteAccount: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Empty = AccountSecurityUiState()
    }
}