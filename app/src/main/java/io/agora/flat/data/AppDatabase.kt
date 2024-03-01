package io.agora.flat.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import io.agora.flat.data.dao.RecordHistoryDao
import io.agora.flat.data.dao.RoomConfigDao
import io.agora.flat.data.model.RecordHistory
import io.agora.flat.data.model.RoomConfig

@Database(
    entities = [RoomConfig::class, RecordHistory::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomConfigDao(): RoomConfigDao

    abstract fun recordHistoryDao(): RecordHistoryDao
}