package io.agora.flat.util

import java.util.*

fun String.fileSuffix(): String {
    return substringAfterLast('.').toLowerCase(Locale.getDefault())
}