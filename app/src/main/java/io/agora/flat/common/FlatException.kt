package io.agora.flat.common

import io.agora.rtm.ErrorInfo

class FlatException constructor(errorCode: Int, message: String, exception: Exception? = null) :
    @JvmOverloads RuntimeException(message, exception) {

    companion object {
        const val RTM_LOGIN_ERROR_START = 0x11000
        const val RTM_LOGOUT_ERROR_START = 0x12000
    }
}

fun ErrorInfo.toFlatException(): FlatException {
    return FlatException(errorCode, errorDescription)
}