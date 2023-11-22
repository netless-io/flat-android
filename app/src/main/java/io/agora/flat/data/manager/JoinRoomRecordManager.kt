package io.agora.flat.data.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.data.model.JoinRoomRecord
import io.agora.flat.data.model.JoinRoomRecordList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JoinRoomRecordManager @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.getSharedPreferences("flat_join_room_record", Context.MODE_PRIVATE)
    private val gson by lazy { Gson() }

    fun addRecord(item: JoinRoomRecord) {
        val items = getRecordList().items
        val updatedItems = items.filterNot { it.uuid == item.uuid }.toMutableList()
        updatedItems.add(0, item)
        store.edit().putString(KEY_JOIN_ROOM_RECORD, gson.toJson(JoinRoomRecordList(updatedItems.take(MAX_RECORD_SIZE)))).apply()
    }

    fun getRecordList(): JoinRoomRecordList {
        val json = store.getString(KEY_JOIN_ROOM_RECORD, null) ?: return JoinRoomRecordList(emptyList())
        return try {
            gson.fromJson(json, JoinRoomRecordList::class.java)
        } catch (e: JsonSyntaxException) {
            JoinRoomRecordList(emptyList())
        }
    }

    fun clearRecords() {
        store.edit().remove(KEY_JOIN_ROOM_RECORD).apply()
    }

    companion object {
        private const val KEY_JOIN_ROOM_RECORD = "join_room_record_key"
        private const val MAX_RECORD_SIZE = 10
    }
}