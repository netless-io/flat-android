package io.agora.flat.ui.util

import java.util.*

data class UiError(val message: String)

class UiMessage(
    val text: String,
    val exception: Throwable? = null,
    val id: Long = UUID.randomUUID().mostSignificantBits,
)
