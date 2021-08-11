package io.agora.flat.util

import android.content.Context
import io.agora.flat.R
import java.util.*

fun String.fileSuffix(): String {
    return substringAfterLast('.').toLowerCase(Locale.getDefault())
}

fun Context.inviteCopyText(
    username: String,
    roomTitle: String,
    roomTime: String,
    roomUUID: String,
    inviteLink: String,
): String {
    return """
                |"${getString(R.string.invite_title_format, username)}"
                |"${getString(R.string.invite_room_name_format, roomTitle)}"
                |"${getString(R.string.invite_begin_time_format, roomTime)}"
                |"${getString(R.string.invite_room_number_format, roomUUID)}"
                |"${getString(R.string.invite_join_link_format, inviteLink)}"
    """.trimMargin()

}