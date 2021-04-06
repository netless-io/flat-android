package link.netless.flat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import link.netless.flat.data.api.RoomService
import link.netless.flat.data.model.RoomInfo
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
            emit(roomAll.data)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun getRoomListHistory(page: Int): Flow<List<RoomInfo>> = flow {
        try {
            val roomAll = roomService.getRoomHistory(page)
            emit(roomAll.data)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }
}