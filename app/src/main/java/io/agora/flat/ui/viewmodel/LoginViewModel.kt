package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.login.LoginUiViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiViewState.Empty)
    val state = _state.asStateFlow()

    fun isLoggedIn(): Boolean {
        return userRepository.isLoggedIn()
    }
}