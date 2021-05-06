package io.agora.flat.data.repository

import io.agora.flat.data.*
import io.agora.flat.data.api.UserService
import io.agora.flat.data.model.RespNoData
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.model.UserInfoWithToken
import io.agora.flat.data.model.WeChatSetAuthIdReq
import io.agora.flat.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter
) {
    suspend fun login(): Result<Boolean> {
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

    suspend fun loginWeChatSetAuthId(authID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.loginWeChatSetAuthId(WeChatSetAuthIdReq(authID))
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

    private fun UserInfoWithToken.mapToUserInfo(): UserInfo {
        return UserInfo(
            name = this.name,
            uuid = this.uuid,
            avatar = this.avatar
        )
    }
}