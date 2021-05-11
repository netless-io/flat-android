package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _mainTab = MutableStateFlow(MainTab.Home)
    val mainTab = _mainTab.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginStart)
    val loginState = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            if (isLoggedIn()) {
                when (userRepository.loginCheck()) {
                    is Success -> {
                        _loginState.value = LoginSuccess
                    }
                    is ErrorResult -> {
                        _loginState.value = LoginError
                    }
                }
            } else {
                _loginState.value = LoginError
            }
        }
    }

    fun onMainTabSelected(selectedTab: MainTab) {
        _mainTab.value = selectedTab
    }

    fun isLoggedIn() = userRepository.isLoggedIn()
}

enum class MainTab {
    // 首页
    Home,

    // 云盘
    CloudStorage
}

sealed class LoginState
internal object LoginStart : LoginState()
internal object LoginSuccess : LoginState()
internal object LoginError : LoginState()