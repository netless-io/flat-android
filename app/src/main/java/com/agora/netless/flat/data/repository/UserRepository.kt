package com.agora.netless.flat.data.repository

import android.content.SharedPreferences
import com.agora.netless.flat.data.api.UserService
import com.agora.netless.flat.data.model.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    private val globalData: SharedPreferences
) {
    fun getUserInfo(): Flow<UserInfo> = flow {
        try {
            emit(userService.getUserInfo().data)
        } catch (e: HttpException) {
            // TODO Wrapper Server Error
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun login(): Flow<Boolean> = flow {
        try {
            globalData.edit().apply {
                putBoolean("key_is_logged_in", true)
            }.apply()
            emit(true)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun isLoggedIn(): Boolean {
        return globalData.getBoolean("key_is_logged_in", false)
    }
}