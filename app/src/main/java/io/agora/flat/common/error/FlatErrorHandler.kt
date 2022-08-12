package io.agora.flat.common.error

import android.content.Context
import io.agora.flat.R
import io.agora.flat.common.*

object FlatErrorHandler {
    fun getStringByError(context: Context, error: Throwable, defaultValue: String = ""): String {
        if (error is FlatException) {
            return when (error) {
                is FlatNetException -> {
                    if (error.exception != null) {
                        context.getString(R.string.error_string_network)
                    } else {
                        when (error.code) {
                            FlatErrorCode.Web_RoomNotFound -> context.getString(R.string.fetcher_room_not_found)
                            FlatErrorCode.Web_RoomIsEnded -> context.getString(R.string.fetcher_room_is_ended)
                            else -> defaultValue
                        }
                    }
                }
                is FlatRtcException -> {
                    defaultValue
                }
                is FlatRtmException -> {
                    defaultValue
                }
            }
        } else {
            return "Unhandled exceptions"
        }
    }
}