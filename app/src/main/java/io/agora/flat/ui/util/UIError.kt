package io.agora.flat.ui.util

class UiError(val message: String)

fun UiError(t: Throwable) = UiError(t.message ?: "Error occurred: $t")