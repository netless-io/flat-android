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
    private var bindings: UserBindings? = null

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

    fun getBindings(): UserBindings? {
        return bindings
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
            userService.requestSmsCode(PhoneReq(phone = phone)).toResult()
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
            userService.requestBindSmsCode(PhoneReq(phone = phone)).toResult()
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

    suspend fun validateDeleteAccount(): Result<RoomCount> {
        return withContext(Dispatchers.IO) {
            userService.validateDeleteAccount().toResult()
        }
    }

    suspend fun deleteAccount(): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.deleteAccount().toResult()
        }
    }

    suspend fun bindingSetAuthUUID(authUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.bindingSetAuthUUID(AuthUUIDReq(authUUID)).toResult()
        }
    }

    suspend fun listBindings(): Result<UserBindings> {
        return withContext(Dispatchers.IO) {
            val result = userService.listBindings().toResult()
            bindings = result.get()
            result
        }
    }

    suspend fun bindWeChat(state: String, code: String): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.bindWeChat(state, code).toResult()
        }
        return when (result) {
            is Success -> {
                bindings = bindings?.copy(wechat = true)
                Success(true)
            }
            is Failure -> {
                Failure(result.throwable, result.error)
            }
        }
    }

    suspend fun bindingProcess(authUUID: String, times: Int = 5): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            repeat(times) {
                val result = userService.bindingProcess(AuthUUIDReq(authUUID)).toResult()
                when (result) {
                    is Success -> {
                        bindings = bindings?.copy(github = true)
                        return@withContext Success(true)
                    }
                    is Failure -> {
                        // state == 1 error; state == 2 loop
                        if (result.error.status == 1) {
                            return@withContext Failure(result.throwable, result.error)
                        }
                    }
                }
                delay(2000)
            }
            return@withContext Failure(RuntimeException("process timeout"))
        }
    }

    suspend fun removeBinding(loginPlatform: LoginPlatform): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val result = userService.removeBinding(RemoveBindingReq(loginPlatform)).toResult()

            return@withContext when (result) {
                is Success -> {
                    appKVCenter.setToken(result.data.token)
                    when (loginPlatform) {
                        LoginPlatform.WeChat -> bindings = bindings?.copy(wechat = false)
                        LoginPlatform.Github -> bindings = bindings?.copy(github = false)
                    }
                    Success(true)
                }
                is Failure -> Failure(result.throwable, result.error)
            }
        }
    }

    private var lastSendCodeTime = 0L

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
