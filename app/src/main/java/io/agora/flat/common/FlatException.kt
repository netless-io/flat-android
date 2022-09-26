package io.agora.flat.common

sealed class FlatException(
    message: String? = null,
    exception: Exception? = null,
) : RuntimeException(message, exception)

data class FlatNetException constructor(
    val exception: Exception? = null,
    val code: Int = DEFAULT_ERROR_CODE,
    val status: Int = DEFAULT_ERROR_STATUS,
) : FlatException(exception = exception) {

    companion object {
        const val DEFAULT_ERROR_STATUS = -1
        const val DEFAULT_ERROR_CODE = -1
        const val DEFAULT_WEB_ERROR_CODE = -2
    }
}

data class FlatRtmException(
    override val message: String = "",
    val code: Int? = null,
) : FlatException(message = message)

data class FlatRtcException(
    val code: Int,
    override val message: String = "",
) : FlatException(message = message)

data class FlatBoardException(
    override val message: String = "",
    val code: Int? = null,
) : FlatException(message = message)
