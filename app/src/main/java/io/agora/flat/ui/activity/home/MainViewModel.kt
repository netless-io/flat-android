package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private var _state = MutableStateFlow(MainViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (isLoggedIn()) {
                _state.value = _state.value.copy(loginState = LoginState.Login)
                _state.value = _state.value.copy(loginCheck = userRepository.loginCheck() is Success)
            } else {
                _state.value = _state.value.copy(loginState = LoginState.Error)
            }
        }
    }

    private fun updateState(loginState: LoginState = LoginState.Init, mainTab: MainTab = MainTab.Home) {
        _state.value = _state.value.copy(loginState = loginState, mainTab = mainTab)
    }

    fun onMainTabSelected(selectedTab: MainTab) {
        _state.value = _state.value.copy(mainTab = selectedTab)
    }

    fun isLoggedIn() = userRepository.isLoggedIn()
}

enum class MainTab {
    // 首页
    Home,

    // 云盘
    CloudStorage
}

data class MainViewState(
    val loginState: LoginState = LoginState.Init,
    val loginCheck: Boolean = false,
    val mainTab: MainTab = MainTab.Home,
)

sealed class LoginState {
    object Init : LoginState()
    object Login : LoginState()
    object Error : LoginState()
}