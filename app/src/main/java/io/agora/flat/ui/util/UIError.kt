package io.agora.flat.ui.util

import java.util.*

class UiError(val message: String)

fun UiError(t: Throwable) = UiError(t.message ?: "Error occurred: $t")

class UiMessage(
    val text: String,
    val id: Long = UUID.randomUUID().mostSignificantBits,
)
