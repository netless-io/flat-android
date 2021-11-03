package io.agora.flat.common

import io.agora.rtm.ErrorInfo

class FlatException @JvmOverloads constructor(
    errorCode: Int,
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

fun ErrorInfo.toFlatException(): FlatException {
    return FlatException(errorCode, errorDescription)
}