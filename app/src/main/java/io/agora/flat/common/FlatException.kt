package io.agora.flat.common

import io.agora.rtm.ErrorInfo

class FlatException constructor(errorCode: Int, message: String, exception: Exception? = null) :
    @JvmOverloads RuntimeException(message, exception)

fun ErrorInfo.toFlatException(): FlatException {
    return FlatException(errorCode, errorDescription)
}