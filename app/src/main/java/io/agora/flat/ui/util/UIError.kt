package io.agora.flat.ui.util

import java.util.*

data class UiError(val message: String)

fun UiError(t: Throwable) = UiError(t.message ?: "Error occurred: $t")

class UiMessage(
    val text: String,
    val exception: Throwable? = null,
    val id: Long = UUID.randomUUID().mostSignificantBits,
)
