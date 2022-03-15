package io.agora.flat.data.repository

import io.agora.flat.data.dao.RoomConfigDao
import io.agora.flat.data.model.RoomConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomConfigRepository @Inject constructor(
    private val roomConfigDao: RoomConfigDao,
) {

    suspend fun updateRoomConfig(roomConfig: RoomConfig) {
        return withContext(Dispatchers.IO) {
            roomConfigDao.insertOrUpdate(roomConfig)
        }
    }

    suspend fun getRoomConfig(uuid: String): RoomConfig? {
        return withContext(Dispatchers.IO) {
            roomConfigDao.getConfigById(uuid)
        }
    }
}