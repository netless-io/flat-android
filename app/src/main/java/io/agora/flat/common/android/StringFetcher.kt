package io.agora.flat.common.android

import android.content.Context
import io.agora.flat.R
import javax.inject.Inject

/**
 * a class for VM get string resource
 */
class StringFetcher @Inject constructor(val context: Context) {
    fun roomNotFound(): String = context.getString(R.string.fetcher_room_not_found)

    fun roomIsEnded(): String = context.getString(R.string.fetcher_room_is_ended)

    fun joinRoomError(code: Int): String = context.getString(R.string.fetcher_join_room_error, code)

    fun startRoomWithRecord(): String = context.getString(R.string.fetcher_start_room_with_record)

    fun loginCodeSend(): String = context.getString(R.string.login_code_send)

    // network
    fun commonFail(): String = context.getString(R.string.error_request_common_fail)
    fun phoneBound(): String = context.getString(R.string.login_phone_already_bound)
    fun alreadyHasPhone(): String = context.getString(R.string.login_already_bind_phone)
    fun invalidVerificationCode(): String = context.getString(R.string.login_verification_code_invalid)
    fun frequentRequest(): String = context.getString(R.string.error_request_too_frequently)
}