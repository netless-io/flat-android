package com.agora.netless.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agora.netless.flat.data.model.UserInfo
import com.agora.netless.flat.data.repository.UserRepository
import com.agora.netless.flat.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {
    val userInfoResource: MutableLiveData<Resource<UserInfo>> by lazy {
        MutableLiveData<Resource<UserInfo>>()
    }

    val loginResource: MutableLiveData<Resource<Boolean>> by lazy {
        MutableLiveData<Resource<Boolean>>()
    }

    fun getUsers() {
        viewModelScope.launch {
            userRepository.getUserInfo().onStart {
                userInfoResource.postValue(Resource.loading())
            }.catch {
                userInfoResource.postValue(
                    Resource.error(
                        null,
                        message = it.message ?: "Error Occurred!",
                        error = Error(it.cause)
                    )
                )
            }.collect {
                userInfoResource.postValue(Resource.success(data = it))
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            userRepository.login().onStart {
                loginResource.postValue(Resource.loading())
            }.catch {
                loginResource.postValue(
                    Resource.error(
                        null,
                        message = it.message ?: "Error Occurred!",
                        error = Error(it.cause)
                    )
                )
            }.collect {
                loginResource.postValue(Resource.success(data = it))
            }
        }
    }

    fun isUserLogin(): Boolean {
        return userRepository.isLoggedIn()
    }
}