package io.agora.flat.data.repository

import android.os.SystemClock
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
                appKVCenter.setUserInfo(result.data.mapToUserInfo())
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
                    appKVCenter.setUserInfo(result.data.mapToUserInfo())
                    return@withContext Success(true)
                }
                delay(2000)
            }
            return@withContext Failure(RuntimeException("process timeout"))
        }
    }

    private fun UserInfoWithToken.mapToUserInfo(): UserInfo {
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
            limitSendCode(Failure(Exception(""), error = Error(1000, 0))) {
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
                appKVCenter.setUserInfo(callResult.data.mapToUserInfo())
                Success(true)
            }
            is Failure -> Failure(callResult.throwable, callResult.error)
        }
    }

    suspend fun requestBindSmsCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestBindSmsCode(PhoneReq(phone = phone)).toResult()
        }
    }

    suspend fun bindPhone(phone: String, code: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.bindPhone(PhoneSmsCodeReq(phone = phone, code = code)).toResult()
        }
    }

    private suspend fun limitSendCode(
        failure: Failure<RespNoData>,
        block: suspend () -> Result<RespNoData>,
    ): Result<RespNoData> {
        return if (SystemClock.elapsedRealtime() - lastSendCodeTime > 60_000) {
            lastSendCodeTime = SystemClock.elapsedRealtime()
            block()
        } else {
            failure
        }
    }
}