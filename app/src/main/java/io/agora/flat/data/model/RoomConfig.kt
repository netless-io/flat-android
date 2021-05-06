package io.agora.flat.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_config")
data class RoomConfig(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "enable_voice") val enableVoice: Boolean,
    @ColumnInfo(name = "enable_camera") val enableCamera: Boolean
)