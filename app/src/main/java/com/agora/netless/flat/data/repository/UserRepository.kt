package com.agora.netless.flat.data.repository

import com.agora.netless.flat.data.AppDataCenter
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
    private val appDataCenter: AppDataCenter
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
            appDataCenter.setUserLoggedIn(true)
            emit(true)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun isLoggedIn(): Boolean {
        return appDataCenter.isUserLoggedIn(false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        return appDataCenter.setUserLoggedIn(loggedIn)
    }
}