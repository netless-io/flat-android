package io.agora.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter
) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    val userInfo: StateFlow<UserInfo?>
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

    suspend fun loginSetAuthUUID(): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            val authUUID = UUID.randomUUID().toString()
            appKVCenter.setAuthUUID(authUUID)

            when (userRepository.loginSetAuthUUID(authUUID)) {
                is Success -> {
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resume(false)
                }
            }
        }
    }

    suspend fun loginProcess(): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (userRepository.loginProcess(appKVCenter.getAuthUUID())) {
                is Success -> cont.resume(true)
                is ErrorResult -> cont.resume(false)
            }
        }
    }
}