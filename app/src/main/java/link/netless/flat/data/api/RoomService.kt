package link.netless.flat.data.api

import link.netless.flat.data.model.BaseReq
import link.netless.flat.data.model.BaseResp
import link.netless.flat.data.model.RoomInfo
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface RoomService {
    @POST("v1/room/list/all")
    suspend fun getRoomAll(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY
    ): BaseResp<List<RoomInfo>>


    @POST("v1/room/list/history")
    suspend fun getRoomHistory(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY
    ): BaseResp<List<RoomInfo>>


}