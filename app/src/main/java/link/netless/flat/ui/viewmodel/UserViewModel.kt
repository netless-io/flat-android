package link.netless.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import link.netless.flat.data.model.UserInfo
import link.netless.flat.data.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    val userInfo: StateFlow<UserInfo>
        get() = _userInfo

    init {

    }

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