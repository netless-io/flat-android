package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountSecurityUiState.Empty)
    val state: StateFlow<AccountSecurityUiState>
        get() = _state;

    init {
        viewModelScope.launch {
            val result = userRepository.validateDeleteAccount()
            if (result is Success) {
                _state.value = _state.value.copy(roomCount = result.data.count)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = userRepository.deleteAccount()
            if (result is Success) {
                userRepository.logout()
                _state.value = _state.value.copy(deleteAccount = true)
            } else {
                showUiMessage(stringFetcher.commonFail())
            }
        }
    }

    private fun showUiMessage(text: String) {
        _state.value = _state.value.copy(uiMessage = UiMessage(text))
    }
}

data class AccountSecurityUiState(
    val roomCount: Int = 0,
    val loading: Boolean = false,
    val deleteAccount: Boolean = false,
    val uiMessage: UiMessage? = null,
) {
    companion object {
        val Empty = AccountSecurityUiState()
    }
}