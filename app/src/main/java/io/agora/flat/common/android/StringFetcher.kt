package io.agora.flat.common.android

import android.content.Context
import io.agora.flat.R
import javax.inject.Inject

/**
 * a class for VM get string resource
 */
class StringFetcher @Inject constructor(val context: Context) {
    fun loginCodeSend(): String = context.getString(R.string.login_code_send)

    fun codeSendSuccess(): String = context.getString(R.string.message_code_send_success)

    // network
    fun commonFail(): String = context.getString(R.string.error_request_common_fail)
    fun phoneBound(): String = context.getString(R.string.login_phone_already_bound)
    fun alreadyHasPhone(): String = context.getString(R.string.login_already_bind_phone)
    fun invalidVerificationCode(): String = context.getString(R.string.login_verification_code_invalid)
    fun frequentRequest(): String = context.getString(R.string.error_request_too_frequently)
}