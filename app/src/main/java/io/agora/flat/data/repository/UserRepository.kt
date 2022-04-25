package io.agora.flat.data.repository

import android.os.SystemClock
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.data.*
import io.agora.flat.data.model.*
import io.agora.flat.http.api.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    private val appKVCenter: AppKVCenter,
) {
    private var lastSendCodeTime = 0L

    suspend fun loginCheck(): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.loginCheck().toResult()
        }
        return when (result) {
            is Success -> {
                appKVCenter.setUserInfo(result.data)
                Success(true)
            }
            is Failure -> {
                Failure(result.throwable, result.error)
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

    fun getUsername(): String {
        return getUserInfo()!!.name
    }

    fun getUserUUID(): String {
        return getUserInfo()!!.uuid
    }

    suspend fun loginSetAuthUUID(authUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.loginSetAuthUUID(AuthUUIDReq(authUUID)).toResult()
        }
    }

    suspend fun loginWeChatCallback(state: String, code: String): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.loginWeChatCallback(state, code).toResult()
        }
        return when (result) {
            is Success -> {
                appKVCenter.setToken(result.data.token)
                appKVCenter.setUserInfo(result.data.toUserInfo())
                Success(true)
            }
            is Failure -> {
                Failure(result.throwable, result.error)
            }
        }
    }

    suspend fun loginProcess(authUUID: String, times: Int = 5): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            repeat(times) {
                val result = userService.loginProcess(AuthUUIDReq(authUUID)).toResult()
                if (result is Success && result.data.token.isNotBlank()) {
                    appKVCenter.setToken(result.data.token)
                    appKVCenter.setUserInfo(result.data.toUserInfo())
                    return@withContext Success(true)
                }
                delay(2000)
            }
            return@withContext Failure(RuntimeException("process timeout"))
        }
    }

    private fun UserInfoWithToken.toUserInfo(): UserInfo {
        return UserInfo(
            name = this.name,
            uuid = this.uuid,
            avatar = this.avatar,
            hasPhone = hasPhone,
        )
    }

    /**
     * phone: +[country code][phone number]
     */
    suspend fun requestLoginSmsCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            limitSendCode {
                userService.requestSmsCode(PhoneReq(phone = phone)).toResult()
            }
        }
    }

    suspend fun loginWithPhone(phone: String, code: String): Result<Boolean> {
        val callResult = withContext(Dispatchers.IO) {
            userService.loginWithPhone(PhoneSmsCodeReq(phone = phone, code = code)).toResult()
        }
        return when (callResult) {
            is Success -> {
                appKVCenter.setToken(callResult.data.token)
                appKVCenter.setUserInfo(callResult.data.toUserInfo())
                Success(true)
            }
            is Failure -> Failure(callResult.throwable, callResult.error)
        }
    }

    suspend fun requestBindSmsCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            limitSendCode {
                userService.requestBindSmsCode(PhoneReq(phone = phone)).toResult()
            }
        }
    }

    suspend fun bindPhone(phone: String, code: String): Result<Boolean> {
        val callResult = withContext(Dispatchers.IO) {
            userService.bindPhone(PhoneSmsCodeReq(phone = phone, code = code)).toResult()
        }
        return when (callResult) {
            is Success -> {
                appKVCenter.getUserInfo()?.let {
                    appKVCenter.setUserInfo(it.copy(hasPhone = true))
                }
                Success(true)
            }
            is Failure -> Failure(callResult.throwable, callResult.error)
        }
    }

    suspend fun rename(name: String): Result<Boolean> {
        val callResult = withContext(Dispatchers.IO) {
            userService.rename(UserRenameReq(name)).toResult()
        }
        return when (callResult) {
            is Success -> {
                appKVCenter.getUserInfo()?.let {
                    appKVCenter.setUserInfo(it.copy(name = name))
                }
                Success(true)
            }
            is Failure -> Failure(callResult.throwable, callResult.error)
        }
    }

    private suspend fun limitSendCode(
        failure: Failure<RespNoData>? = null,
        block: suspend () -> Result<RespNoData>,
    ): Result<RespNoData> {
        return if (SystemClock.elapsedRealtime() - lastSendCodeTime > 60_000) {
            lastSendCodeTime = SystemClock.elapsedRealtime()
            block()
        } else {
            failure ?: Failure(Exception("limit send code"), Error(404, FlatErrorCode.Web_ExhaustiveAttack))
        }
    }
}