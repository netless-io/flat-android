package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.model.*
import io.agora.flat.data.toResult
import io.agora.flat.http.api.RoomService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomService: RoomService,
) {
    suspend fun getRoomListAll(page: Int): Result<List<RoomInfo>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomAll(page).toResult()
        }
    }

    suspend fun getRoomListHistory(page: Int): Result<List<RoomInfo>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomHistory(page).toResult()
        }
    }

    suspend fun getOrdinaryRoomInfo(roomUUID: String): Result<RoomDetailOrdinary> {
        return withContext(Dispatchers.IO) {
            roomService.getOrdinaryRoomInfo(RoomDetailOrdinaryReq(roomUUID = roomUUID))
                .toResult()
        }
    }

    suspend fun getPeriodicRoomInfo(periodicUUID: String): Result<RoomDetailPeriodic> {
        return withContext(Dispatchers.IO) {
            roomService.getPeriodicRoomInfo(RoomDetailPeriodicReq(periodicUUID = periodicUUID))
                .toResult()
        }
    }

    suspend fun cancelOrdinary(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelOrdinary(CancelRoomReq(roomUUID = roomUUID))
                .toResult()
        }
    }

    suspend fun cancelPeriodic(periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodic(CancelRoomReq(periodicUUID = periodicUUID))
                .toResult()
        }
    }

    suspend fun cancelPeriodicSubRoom(roomUUID: String, periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodicSubRoom(CancelRoomReq(roomUUID, periodicUUID))
                .toResult()
        }
    }

    suspend fun joinRoom(uuid: String): Result<RoomPlayInfo> {
        return withContext(Dispatchers.IO) {
            roomService.joinRoom(JoinRoomReq(uuid)).toResult()
        }
    }

    suspend fun getRoomUsers(roomUUID: String, usersUUID: List<String>?): Result<Map<String, NetworkRoomUser>> {
        return withContext(Dispatchers.IO) {
            roomService.getRoomUsers(RoomUsersReq(roomUUID, usersUUID)).toResult()
        }
    }

    suspend fun createOrdinary(title: String, type: RoomType): Result<RoomCreateRespData> {
        return withContext(Dispatchers.IO) {
            roomService.createOrdinary(RoomCreateReq(title, type, System.currentTimeMillis())).toResult()
        }
    }

    suspend fun startRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.startRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun pauseRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.pauseRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun stopRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.stopRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }
}