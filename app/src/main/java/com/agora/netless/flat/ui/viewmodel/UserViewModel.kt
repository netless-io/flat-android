package com.agora.netless.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.agora.netless.flat.data.repository.UserRepository
import com.agora.netless.flat.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {

    fun getUsers() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        userRepository.getUserInfo().catch {
            emit(Resource.error(null, message = it.message ?: "Error Occurred!", error = Error(it.cause)))
        }.collect {
            emit(Resource.success(data = it))
        }
    }
}