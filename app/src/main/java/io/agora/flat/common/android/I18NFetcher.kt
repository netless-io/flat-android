package io.agora.flat.common.android

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class I18NFetcher @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        const val JOIN_ROOM_RECORD_PMI_TITLE = "join_room_record_pmi_title"

        val map = mapOf(
            JOIN_ROOM_RECORD_PMI_TITLE to R.string.join_room_record_pmi_title
        )
    }

    private val resources = context.resources

    fun getString(key: String): String {
        val resId = map[key] ?: return ""
        return resources.getString(resId)
    }

    fun getString(key: String, vararg formatArgs: Any?): String {
        val resId = map[key] ?: return ""
        return resources.getString(resId, *formatArgs)
    }
}