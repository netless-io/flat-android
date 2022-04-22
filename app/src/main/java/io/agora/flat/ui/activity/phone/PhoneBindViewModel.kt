package io.agora.flat.ui.activity.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBindViewModel @Inject constructor(
    private val userRepository: UserRepository,
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
            userRepository.requestBindSmsCode(phone = phone);
        }
    }

    fun bindPhone(phone: String, code: String) {
        viewModelScope.launch {
            // 错误处理
            userRepository.bindPhone(phone = phone, code = code);
        }
    }
}