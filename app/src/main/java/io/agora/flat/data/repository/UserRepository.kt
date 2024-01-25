package io.agora.flat.data.repository

import io.agora.flat.common.FlatNetException
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Result
import io.agora.flat.data.Success
import io.agora.flat.data.model.AuthUUIDReq
import io.agora.flat.data.model.EmailBindReq
import io.agora.flat.data.model.EmailCodeReq
import io.agora.flat.data.model.EmailPasswordReq
import io.agora.flat.data.model.EmailRegisterReq
import io.agora.flat.data.model.LoginHistoryItem
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.PhonePasswordReq
import io.agora.flat.data.model.PhoneRegisterReq
import io.agora.flat.data.model.PhoneReq
import io.agora.flat.data.model.PhoneSmsCodeReq
import io.agora.flat.data.model.RemoveBindingReq
import io.agora.flat.data.model.RespNoData
import io.agora.flat.data.model.RoomCount
import io.agora.flat.data.model.SetPasswordReq
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.model.UserInfoWithToken
import io.agora.flat.data.model.UserRenameReq
import io.agora.flat.data.onSuccess
import io.agora.flat.data.toResult
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.http.api.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    private val appKVCenter: AppKVCenter,
    private val logger: Logger,
) {
    private var bindings: UserBindings? = null

    suspend fun loginCheck(): Result<Boolean> {
        if (AppKVCenter.MockData.mockEnable) {
            return Success(true)
        }
        val result = withContext(Dispatchers.IO) {
            userService.loginCheck().toResult()
        }
        return when (result) {
            is Success -> {
                updateUserAndToken(result)
                appKVCenter.updateSessionId(UUID.randomUUID().toString())
                logger.setUserId(result.data.uuid)
                Success(true)
            }

            is Failure -> Failure(result.exception)
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

    fun getUserAvatar(): String {
        return getUserInfo()!!.avatar
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
                updateUserAndToken(result)
                Success(true)
            }

            is Failure -> Failure(result.exception)
        }
    }

    suspend fun loginProcess(authUUID: String, times: Int = 10): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            repeat(times) {
                val result = userService.loginProcess(AuthUUIDReq(authUUID)).toResult()
                if (result is Success && result.data.token.isNotBlank()) {
                    updateUserAndToken(result)
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
            hasPassword = this.hasPassword,
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
        val result = withContext(Dispatchers.IO) {
            userService.loginWithPhone(PhoneSmsCodeReq(phone = phone, code = code)).toResult()
        }
        return when (result) {
            is Success -> {
                updateUserAndToken(result)
                Success(true)
            }

            is Failure -> Failure(result.exception)
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
                updateBindingState()
                Success(true)
            }

            is Failure -> Failure(callResult.exception)
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

            is Failure -> Failure(callResult.exception)
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
                updateBindingState()
                Success(true)
            }

            is Failure -> {
                Failure(result.exception)
            }
        }
    }

    suspend fun bindingProcess(authUUID: String, times: Int = 5): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            repeat(times) {
                when (val result = userService.bindingProcess(AuthUUIDReq(authUUID)).toResult()) {
                    is Success -> {
                        updateBindingState()
                        return@withContext Success(true)
                    }

                    is Failure -> {
                        // state == 1 error; state == 2 loop
                        val exception = result.exception as FlatNetException
                        if (exception.status == 1) {
                            return@withContext Failure(result.exception)
                        }
                    }
                }
                delay(2000)
            }
            return@withContext Failure(RuntimeException("process timeout"))
        }
    }

    suspend fun requestBindEmailCode(email: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestBindEmailCode(EmailCodeReq(email, LanguageManager.currentLocale().language)).toResult()
        }
    }

    suspend fun bindEmail(email: String, code: String): Result<Boolean> {
        val result = withContext(Dispatchers.IO) {
            userService.bindEmail(EmailBindReq(email, code)).toResult()
        }
        return when (result) {
            is Success -> {
                updateBindingState()
                Success(true)
            }

            is Failure -> {
                Failure(result.exception)
            }
        }
    }

    private suspend fun updateBindingState() {
        withContext(Dispatchers.IO) {
            userService.listBindings().toResult().onSuccess {
                bindings = it
            }
        }
    }

    suspend fun removeBinding(loginPlatform: LoginPlatform): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val result = userService.removeBinding(RemoveBindingReq(loginPlatform)).toResult()) {
                is Success -> {
                    appKVCenter.setToken(result.data.token)
                    bindings = when (loginPlatform) {
                        LoginPlatform.WeChat -> bindings?.copy(wechat = false)
                        LoginPlatform.Github -> bindings?.copy(github = false)
                        LoginPlatform.Apple -> bindings?.copy(apple = false)
                        LoginPlatform.Google -> bindings?.copy(google = false)
                        LoginPlatform.Email -> bindings?.copy(email = false)
                        LoginPlatform.Phone -> bindings?.copy(phone = false)
                    }
                    Success(true)
                }

                is Failure -> Failure(result.exception)
            }
        }
    }

//    private var lastSendCodeTime = 0L
//
//    private suspend fun limitSendCode(
//        failure: Failure<RespNoData>? = null,
//        block: suspend () -> Result<RespNoData>,
//    ): Result<RespNoData> {
//        return if (SystemClock.elapsedRealtime() - lastSendCodeTime > 60_000) {
//            lastSendCodeTime = SystemClock.elapsedRealtime()
//            block()
//        } else {
//            failure ?: Failure(
//                FlatNetException(
//                    RuntimeException("limit send code"), status = 404, code = FlatErrorCode.Web.ExhaustiveAttack
//                )
//            )
//        }
//    }

    suspend fun requestRegisterSmsCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestRegisterSmsCode(PhoneReq(phone)).toResult()
        }
    }

    suspend fun registerWithPhone(phone: String, code: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val result = userService.registerWithPhone(PhoneRegisterReq(phone, code, password)).toResult()) {
                is Success -> {
                    updateUserAndToken(result)
                    appKVCenter.addLoginHistoryItem(LoginHistoryItem(phone, password))
                    Success(true)
                }

                is Failure -> Failure(result.exception)
            }
        }
    }

    suspend fun requestRegisterEmailCode(email: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestRegisterEmailCode(EmailCodeReq(email, LanguageManager.currentLocale().language))
                .toResult()
        }
    }

    suspend fun registerWithEmail(email: String, code: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val result = userService.registerWithEmail(
                EmailRegisterReq(
                    email = email,
                    code = code,
                    password = password
                )
            ).toResult()) {
                is Success -> {
                    updateUserAndToken(result)
                    appKVCenter.addLoginHistoryItem(LoginHistoryItem(email, password))
                    Success(true)
                }

                is Failure -> Failure(result.exception)
            }
        }
    }


    suspend fun loginWithPhonePassword(phone: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val result = userService.loginWithPhonePassword(PhonePasswordReq(phone, password)).toResult()) {
                is Success -> {
                    updateUserAndToken(result)
                    appKVCenter.addLoginHistoryItem(LoginHistoryItem(phone, password))
                    Success(true)
                }

                is Failure -> Failure(result.exception)
            }
        }
    }

    suspend fun loginWithEmailPassword(email: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val result = userService.loginWithEmailPassword(EmailPasswordReq(email, password)).toResult()) {
                is Success -> {
                    updateUserAndToken(result)
                    appKVCenter.addLoginHistoryItem(LoginHistoryItem(email, password))
                    Success(true)
                }

                is Failure -> Failure(result.exception)
            }
        }
    }

    private fun updateUserAndToken(result: Success<UserInfoWithToken>) {
        appKVCenter.setToken(result.data.token)
        appKVCenter.setUserInfo(result.data.toUserInfo())
    }

    suspend fun requestResetEmailCode(email: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestResetEmailCode(EmailCodeReq(email, LanguageManager.currentLocale().language)).toResult()
        }
    }

    suspend fun resetWithEmail(email: String, code: String, password: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.resetEmailPassword(EmailRegisterReq(email, code, password)).toResult()
        }
    }

    suspend fun requestResetPhoneCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestResetPhoneCode(PhoneReq(phone)).toResult()
        }
    }

    suspend fun resetWithPhone(phone: String, code: String, password: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.resetPhonePassword(PhoneRegisterReq(phone, code, password)).toResult()
        }
    }

    suspend fun requestRebindPhoneCode(phone: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.requestRebindPhoneCode(PhoneReq(phone)).toResult()
        }
    }

    suspend fun rebindWithPhone(phone: String, code: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.rebindPhone(PhoneSmsCodeReq(phone, code))
                .toResult()
                .onSuccess {
                    appKVCenter.setToken(it.token)
                    appKVCenter.setUserInfo(it.toUserInfo())
                    // bindings = it.bindings
                }
                .toNoData()
        }
    }

    suspend fun setPassword(password: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.setPassword(SetPasswordReq(null, password)).toResult()
                .onSuccess {
                    appKVCenter.getUserInfo()?.let { userInfo ->
                        appKVCenter.setUserInfo(userInfo.copy(hasPassword = true))
                    }
                }
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            userService.setPassword(SetPasswordReq(oldPassword, newPassword)).toResult()
        }
    }

    private inline fun <T> Result<T>.toNoData(): Result<RespNoData> {
        return when (this) {
            is Success -> Success(RespNoData)
            is Failure -> Failure(this.exception)
        }
    }
}
