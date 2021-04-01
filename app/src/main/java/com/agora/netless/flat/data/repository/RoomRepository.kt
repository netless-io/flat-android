package com.agora.netless.flat.data.repository

import com.agora.netless.flat.data.api.RoomService
import com.agora.netless.flat.data.model.RoomInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomService: RoomService,
) {
    fun getRoomListAll(page: Int): Flow<List<RoomInfo>> = flow {
        try {
            val roomAll = roomService.getRoomAll(page)
            print("getRoomListAll $roomAll")
            emit(roomAll.data)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun getHistoryRecord(page: Int): Flow<List<RoomInfo>> = flow {
        try {
            val roomAll = roomService.getRoomHistory(page)
            print("getHistoryRecord $roomAll")
            emit(roomAll.data)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }
}