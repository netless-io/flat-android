package io.agora.flat.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.agora.flat.data.dao.RoomConfigDao
import io.agora.flat.data.model.RoomConfig

@Database(entities = [RoomConfig::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomConfigDao(): RoomConfigDao
}