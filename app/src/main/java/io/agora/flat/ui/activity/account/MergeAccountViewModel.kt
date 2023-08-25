package io.agora.flat.ui.activity.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
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
class MergeAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    private val uiMessageManager = UiMessageManager()
    private val bindingState = ObservableLoadingCounter()
    private val bindSuccess = MutableStateFlow(false)

    private var _state = MutableStateFlow(MergeAccountUiState.Empty)
    val state: StateFlow<MergeAccountUiState>
        get() = _state

    private val phone: String = checkNotNull(savedStateHandle[Constants.IntentKey.PHONE])
    private val ccode: String = checkNotNull(savedStateHandle[Constants.IntentKey.CALLING_CODE])

    init {
        viewModelScope.launch {
            combine(bindSuccess, bindingState.observable, uiMessageManager.message) { bindSuccess, binding, message ->
                MergeAccountUiState(
                    bindSuccess = bindSuccess,
                    binding = binding,
                    message = message,
                    phone = phone,
                    ccode = ccode,
                )
            }.collect {
                _state.value = it
            }
        }

        sendPhoneCode("$ccode$phone")
    }

    fun sendPhoneCode(phone: String) {
        viewModelScope.launch {
            userRepository.requestRebindPhoneCode(phone = phone)
                .onSuccess {
                    showUiMessage(stringFetcher.loginCodeSend())
                }.onFailure {
                    uiMessageManager.emitMessage(UiErrorMessage(it))
                }
        }
    }

    fun mergeAccountByPhone(phone: String, code: String) {
        viewModelScope.launch {
            bindingState.addLoader()
            userRepository.rebindWithPhone(phone = phone, code = code).onSuccess {
                bindSuccess.value = true
            }.onFailure {
                uiMessageManager.emitMessage(UiErrorMessage(it))
            }
            bindingState.removeLoader()
        }
    }

    private fun showUiMessage(message: String) {
        _state.value = _state.value.copy(message = UiMessage(message))
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class MergeAccountUiState(
    val bindSuccess: Boolean,
    val binding: Boolean,
    val message: UiMessage?,

    val phone: String = "",
    val ccode: String = "",
) {
    companion object {
        val Empty = MergeAccountUiState(
            bindSuccess = false,
            binding = false,
            message = null,
        )
    }
}