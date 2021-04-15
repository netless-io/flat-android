package link.netless.flat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.netless.flat.data.*
import link.netless.flat.data.api.UserService
import link.netless.flat.data.model.RespNoData
import link.netless.flat.data.model.UserInfo
import link.netless.flat.data.model.UserInfoWithToken
import link.netless.flat.data.model.WeChatSetAuthIdReq
import link.netless.flat.di.AppModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    @AppModule.GlobalData private val appDataCenter: AppDataCenter
) {
    suspend fun login(): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.loginCheck().executeOnce().toResult()
        }
        return when (result) {
            is Success -> {
                appDataCenter.setUserInfo(result.data)
                Success(true)
            }
            is ErrorResult -> {
                ErrorResult(result.throwable, result.error)
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return appDataCenter.isUserLoggedIn()
    }

    fun logout() {
        appDataCenter.setLogout()
    }

    fun getUserInfo(): UserInfo? {
        return appDataCenter.getUserInfo()
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
                appDataCenter.setToken(result.data.token)
                appDataCenter.setUserInfo(result.data.mapToUserInfo())
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
            sex = this.sex,
            uuid = this.uuid,
            avatar = this.avatar
        )
    }
}