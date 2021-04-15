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

    suspend fun cancelOrdinary(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelOrdinary(CancelRoomReq(roomUUID = roomUUID))
                .executeOnce().toResult()
        }
    }

    suspend fun cancelPeriodic(periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodic(CancelRoomReq(periodicUUID = periodicUUID))
                .executeOnce().toResult()
        }
    }

    suspend fun cancelPeriodicSubRoom(roomUUID: String, periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodicSubRoom(CancelRoomReq(roomUUID, periodicUUID))
                .executeOnce().toResult()
        }
    }

    suspend fun joinRoom(uuid: String): Result<RoomPlayInfo> {
        return withContext(Dispatchers.IO) {
            roomService.joinRoom(JoinRoomReq(uuid))
                .executeOnce().toResult()
        }
    }

    suspend fun getRoomUsers(roomUUID: String, usersUUID: List<String>): Result<Map<String, RtcUser>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomUsers(RoomUsersReq(roomUUID, usersUUID))
                .executeOnce().toResult()
        }
    }
}