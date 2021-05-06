package io.agora.flat.data.dao

import androidx.room.*
import io.agora.flat.data.model.RoomConfig

@Dao
interface RoomConfigDao {
    @Query("SELECT * FROM room_config")
    fun getAll(): List<RoomConfig>

    @Query("SELECT * FROM room_config WHERE uuid is :uuid")
    fun getConfigById(uuid: String): RoomConfig?

    @Insert
    fun insert(roomConfig: RoomConfig)

    @Update
    fun update(roomConfig: RoomConfig)

    @Delete
    fun delete(roomConfig: RoomConfig)

    fun insertOrUpdate(roomConfig: RoomConfig) {
        val existConfig = getConfigById(roomConfig.uuid)
        if (existConfig != null) {
            update(roomConfig)
        } else {
            insert(roomConfig)
        }
    }
}