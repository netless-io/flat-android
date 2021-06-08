package io.agora.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    val userInfo: StateFlow<UserInfo?>
        get() = _userInfo

    val loggedInData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(isLoggedIn())
    }

    fun isLoggedIn(): Boolean {
        return userRepository.isLoggedIn()
    }

    fun logout() {
        userRepository.logout()
        loggedInData.value = false
    }
}