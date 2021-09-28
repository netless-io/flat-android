package io.agora.flat.common.android

import android.content.Context
import io.agora.flat.R
import javax.inject.Inject

class StringFetcher @Inject constructor(val context: Context) {
    fun roomNotFound(): String = context.getString(R.string.fetcher_room_not_found)

    fun roomIsEnded(): String = context.getString(R.string.fetcher_room_is_ended)

    fun joinRoomError(code: Int): String = context.getString(R.string.fetcher_join_room_error, code)

    fun startRoomWithRecord(): String = context.getString(R.string.fetcher_start_room_with_record)
}