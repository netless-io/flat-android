package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.RoomServiceFetcher
import io.agora.flat.data.model.CancelRoomReq
import io.agora.flat.data.model.JoinRoomReq
import io.agora.flat.data.model.NetworkRoomUser
import io.agora.flat.data.model.PureRoomReq
import io.agora.flat.data.model.RespNoData
import io.agora.flat.data.model.RoomCreateReq
import io.agora.flat.data.model.RoomCreateRespData
import io.agora.flat.data.model.RoomDetailOrdinary
import io.agora.flat.data.model.RoomDetailOrdinaryReq
import io.agora.flat.data.model.RoomDetailPeriodic
import io.agora.flat.data.model.RoomDetailPeriodicReq
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.model.RoomUsersReq
import io.agora.flat.data.toResult
import io.agora.flat.http.api.RoomService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomService: RoomService,
    private val roomServiceFetcher: RoomServiceFetcher
) {
    private fun fetchService(uuid: String): RoomService {
        return roomServiceFetcher.fetch(uuid)
    }

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
            fetchService(roomUUID).getOrdinaryRoomInfo(RoomDetailOrdinaryReq(roomUUID = roomUUID)).toResult()
        }
    }

    suspend fun getPeriodicRoomInfo(periodicUUID: String): Result<RoomDetailPeriodic> {
        return withContext(Dispatchers.IO) {
            roomService.getPeriodicRoomInfo(RoomDetailPeriodicReq(periodicUUID = periodicUUID)).toResult()
        }
    }

    suspend fun cancelOrdinary(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).cancelOrdinary(CancelRoomReq(roomUUID = roomUUID)).toResult()
        }
    }

    suspend fun cancelPeriodic(periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodic(CancelRoomReq(periodicUUID = periodicUUID)).toResult()
        }
    }

    suspend fun cancelPeriodicSubRoom(roomUUID: String, periodicUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            roomService.cancelPeriodicSubRoom(CancelRoomReq(roomUUID, periodicUUID)).toResult()
        }
    }

    suspend fun joinRoom(roomUUID: String): Result<RoomPlayInfo> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).joinRoom(JoinRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun getRoomUsers(roomUUID: String, usersUUID: List<String>?): Result<Map<String, NetworkRoomUser>> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).getRoomUsers(RoomUsersReq(roomUUID, usersUUID)).toResult()
        }
    }

    suspend fun createOrdinary(title: String, type: RoomType): Result<RoomCreateRespData> {
        return withContext(Dispatchers.IO) {
            roomService.createOrdinary(RoomCreateReq(title, type, System.currentTimeMillis())).toResult()
        }
    }

    suspend fun startRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).startRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun pauseRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).pauseRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun stopRoomClass(roomUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            fetchService(roomUUID).stopRoomClass(PureRoomReq(roomUUID)).toResult()
        }
    }
}