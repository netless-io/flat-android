package io.agora.flat.data.dao

import androidx.room.*
import io.agora.flat.data.model.RecordHistory

@Dao
interface RecordHistoryDao {
    @Query("SELECT * FROM record_history")
    suspend fun getAll(): List<RecordHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordHistory: RecordHistory)

    @Query("SELECT * FROM record_history WHERE roomUuid = :roomUuid")
    suspend fun getByRoomUuid(roomUuid: String): List<RecordHistory>

    @Query("SELECT * FROM record_history WHERE resourceId = :resourceId")
    suspend fun getByResourceId(resourceId: String): List<RecordHistory>

    @Query("DELETE FROM record_history WHERE roomUuid = :roomUuid")
    suspend fun deleteByRoomUuid(roomUuid: String)

    @Query("DELETE FROM record_history WHERE resourceId = :resourceId")
    suspend fun deleteByResourceId(resourceId: String)
}