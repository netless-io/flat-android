package io.agora.flat.common.error

import android.content.Context
import io.agora.flat.R
import io.agora.flat.common.*

object FlatErrorHandler {
    fun getErrorStr(context: Context, error: Throwable?, defaultValue: String = "Unhandled exceptions"): String {
        return if (error is FlatException) {
            when (error) {
                is FlatNetException -> getNetErrorString(context, error)

                is FlatRtcException -> defaultValue

                is FlatRtmException -> defaultValue

                else -> defaultValue
            }
        } else {
            defaultValue
        }
    }

    private fun getNetErrorString(context: Context, error: FlatNetException): String {
        return when (error.code) {
            FlatErrorCode.Web.ParamsCheckFailed -> context.getString(R.string.error_params_check_failed)

            FlatErrorCode.Web.RoomNotFound -> context.getString(R.string.fetcher_room_not_found)
            FlatErrorCode.Web.RoomIsEnded -> context.getString(R.string.fetcher_room_is_ended)

            FlatErrorCode.Web.SMSVerificationCodeInvalid -> context.getString(R.string.login_verification_code_invalid)
            FlatErrorCode.Web.ExhaustiveAttack -> context.getString(R.string.error_request_too_frequently)

            FlatErrorCode.Web.UserNotFound -> context.getString(R.string.error_user_not_found)
            FlatErrorCode.Web.UserRoomListNotEmpty -> context.getString(R.string.error_user_room_list_not_empty)
            FlatErrorCode.Web.UserAlreadyBinding -> context.getString(R.string.error_user_already_binding)
            FlatErrorCode.Web.UserPasswordIncorrect -> context.getString(R.string.error_user_password_incorrect)
            FlatErrorCode.Web.UserOrPasswordIncorrect -> context.getString(R.string.error_user_or_password_incorrect)

            FlatErrorCode.Web.SMSVerificationCodeInvalid -> context.getString(R.string.error_verification_code_invalid)
            FlatErrorCode.Web.SMSAlreadyExist -> context.getString(R.string.error_phone_already_exist)
            FlatErrorCode.Web.SMSAlreadyBinding -> context.getString(R.string.error_phone_already_binding)
            FlatErrorCode.Web.SMSFailedToSendCode -> context.getString(R.string.error_send_verification_code)

            FlatErrorCode.Web.EmailVerificationCodeInvalid -> context.getString(R.string.error_email_verification_code_invalid)
            FlatErrorCode.Web.EmailAlreadyExist -> context.getString(R.string.error_email_already_exist)
            FlatErrorCode.Web.EmailAlreadyBinding -> context.getString(R.string.error_email_already_binding)
            FlatErrorCode.Web.EmailFailedToSendCode -> context.getString(R.string.error_email_failed_to_send_code)

            else -> context.getString(R.string.error_string_network)
        }
    }
}