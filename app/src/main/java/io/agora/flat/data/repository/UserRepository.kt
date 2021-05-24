package io.agora.flat.data.repository

import io.agora.flat.data.*
import io.agora.flat.data.api.UserService
import io.agora.flat.data.model.AuthUUIDReq
import io.agora.flat.data.model.RespNoData
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.model.UserInfoWithToken
import io.agora.flat.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter
) {
    suspend fun loginCheck(): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.loginCheck().executeOnce().toResult()
        }
        return when (result) {
            is Success -> {
                appKVCenter.setUserInfo(result.data)
                Success(true)
            }
            is ErrorResult -> {
                ErrorResult(result.throwable, result.error)
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return appKVCenter.isUserLoggedIn()
    }

    fun logout() {
        appKVCenter.setLogout()
    }

    fun getUserInfo(): UserInfo? {
        return appKVCenter.getUserInfo()
    }

    suspend fun loginSetAuthUUID(authUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.loginSetAuthUUID(AuthUUIDReq(authUUID))
                .executeOnce().toResult()
        }
    }

    suspend fun loginWeChatCallback(state: String, code: String): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.loginWeChatCallback(state, code)
                .executeOnce().toResult()
        }
        return when (result) {
            is Success -> {
                appKVCenter.setToken(result.data.token)
                appKVCenter.setUserInfo(result.data.mapToUserInfo())
                Success(true)
            }
            is ErrorResult -> {
                ErrorResult(result.throwable, result.error)
            }
        }
    }

    suspend fun loginProcess(authUUID: String, times: Int = 20): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            repeat(times) {
                val result =
                    userService.loginProcess(AuthUUIDReq(authUUID)).executeOnce().toResult()
                if (result is Success && result.data.token.isNotBlank()) {
                    appKVCenter.setToken(result.data.token)
                    appKVCenter.setUserInfo(result.data.mapToUserInfo())
                    return@withContext Success(true)
                }
                delay(2000)
            }
            return@withContext ErrorResult(RuntimeException("process timeout"))
        }
    }

    private fun UserInfoWithToken.mapToUserInfo(): UserInfo {
        return UserInfo(
            name = this.name,
            uuid = this.uuid,
            avatar = this.avatar
        )
    }
}