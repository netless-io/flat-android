package io.agora.flat.common

import io.agora.rtm.ErrorInfo

class FlatException @JvmOverloads constructor(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

fun ErrorInfo.toFlatException(): FlatException {
    return FlatException(toString())
}