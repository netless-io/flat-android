package link.netless.flat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.netless.flat.data.Result
import link.netless.flat.data.api.RoomService
import link.netless.flat.data.executeOnce
import link.netless.flat.data.model.*
import link.netless.flat.data.toResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomService: RoomService
) {
    suspend fun getRoomListAll(page: Int): Result<List<RoomInfo>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomAll(page).executeOnce().toResult()
        }
    }

    suspend fun getRoomListHistory(page: Int): Result<List<RoomInfo>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomHistory(page).executeOnce().toResult()
        }
    }

    suspend fun getOrdinaryRoomInfo(roomUUID: String): Result<RoomDetailOrdinary> {
        return withContext(Dispatchers.IO) {
            roomService.getOrdinaryRoomInfo(RoomDetailOrdinaryReq(roomUUID = roomUUID))
                .executeOnce().toResult()
        }
    }

    suspend fun getPeriodicRoomInfo(periodicUUID: String): Result<RoomDetailPeriodic> {
        return withContext(Dispatchers.IO) {
            roomService.getPeriodicRoomInfo(RoomDetailPeriodicReq(periodicUUID = periodicUUID))
                .executeOnce().toResult()
        }
    }
}