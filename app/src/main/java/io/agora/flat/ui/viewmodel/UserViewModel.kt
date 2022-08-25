package io.agora.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.Success
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserUpdated
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    val userInfo: StateFlow<UserInfo?>
        get() = _userInfo

    val loggedInData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(userRepository.isLoggedIn())
    }

    fun refreshUser() {
        viewModelScope.launch {
            _userInfo.value = userRepository.getUserInfo()
        }
    }

    fun logout() {
        userRepository.logout()
        loggedInData.value = false
    }

    suspend fun rename(name: String): Boolean {
        val result = userRepository.rename(name = name)
        if (result is Success) {
            eventBus.produceEvent(UserUpdated)
        }
        return result is Success
    }
}