package io.agora.flat.util

import io.agora.flat.data.model.CoursewareType
import java.util.*

fun String.fileSuffix(): String {
    return substringAfterLast('.').lowercase(Locale.getDefault())
}

fun String.coursewareType(): CoursewareType {
    return when (fileSuffix()) {
        "jpg", "jpeg", "png", "webp" -> {
            CoursewareType.Image
        }
        "doc", "docx", "pdf" -> {
            CoursewareType.DocStatic
        }
        "ppt", "pptx" -> {
            CoursewareType.DocDynamic
        }
        "mp3" -> {
            CoursewareType.Audio
        }
        "mp4" -> {
            CoursewareType.Video
        }
        else -> {
            CoursewareType.Unknown
        }
    }
}

fun String.isDynamicDoc() : Boolean {
    return this.coursewareType() == CoursewareType.DocDynamic
}

fun String.toInviteCodeDisplay() = if (length == 10) {
    "${substring(IntRange(0, 2))} ${substring(IntRange(3, 5))} ${substring(IntRange(6, 9))}"
} else {
    this
}

internal const val ROOM_UUID_PATTERN = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""
internal const val INVITE_CODE_PATTERN = """[0-9]{3} [0-9a-fA-F]{3} [0-9a-fA-F]{4}"""

fun String.parseRoomID(): String? {
    if (this.isBlank()) {
        return null
    }
    return parseInviteCode(this) ?: parseRoomUUID(this)
}

internal fun parseInviteCode(text: CharSequence): String? {
    val regex = INVITE_CODE_PATTERN.toRegex()
    val entire = regex.find(text)
    return entire?.value
}

internal fun parseRoomUUID(text: CharSequence): String? {
    val regex = ROOM_UUID_PATTERN.toRegex()
    val entire = regex.find(text)
    return entire?.value
}

internal const val PHONE_PATTERN = """1[3456789]\d{9}"""

fun String.isValidPhone(): Boolean {
    return PHONE_PATTERN.toRegex().matches(this)
}

fun String.isValidSmsCode(): Boolean {
    return this.length == 6
}