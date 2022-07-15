package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.impl.EventBus
import io.agora.flat.event.UserBindingsUpdated
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    private val _userBindings = MutableStateFlow<UserBindings?>(null)

    private val _state = MutableStateFlow(UserInfoUiState())
    val state: StateFlow<UserInfoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_userInfo, _userBindings) { userInfo, bindings ->
                UserInfoUiState(userInfo, bindings)
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<UserBindingsUpdated>().collect {
                _userBindings.value = userRepository.getBindings()
            }
        }

        loadBindings()
    }

    private fun loadBindings() {
        viewModelScope.launch {
            _userBindings.value = userRepository.listBindings().get()
        }
    }

    fun refreshUser() {
        viewModelScope.launch {

        }
    }

    fun processAction(action: UserInfoUiAction) {
        viewModelScope.launch {
            when (action) {
                UserInfoUiAction.UnbindGithub -> userRepository.removeBinding(LoginPlatform.Github)
                UserInfoUiAction.UnbindWeChat -> userRepository.removeBinding(LoginPlatform.WeChat)
            }
            _userBindings.value = userRepository.getBindings()
        }
    }
}

data class UserInfoUiState(
    val userInfo: UserInfo? = null,
    val bindings: UserBindings? = null,
    val loading: Boolean = true,
)

sealed class UserInfoUiAction {
    object UnbindWeChat : UserInfoUiAction()
    object UnbindGithub : UserInfoUiAction()
    object BindGithub : UserInfoUiAction()
    object BindWeChat : UserInfoUiAction()
}
