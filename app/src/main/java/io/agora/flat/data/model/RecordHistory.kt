package io.agora.flat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record_history")
data class RecordHistory(
    val roomUuid: String,
    @PrimaryKey
    val resourceId: String,
    val sid: String,
    val mode: AgoraRecordMode = AgoraRecordMode.Mix,
)