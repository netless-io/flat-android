package link.netless.flat.data.api

import link.netless.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface RoomService {
    @POST("v1/room/list/all")
    fun getRoomAll(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY
    ): Call<BaseResp<List<RoomInfo>>>

    @POST("v1/room/list/history")
    fun getRoomHistory(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY
    ): Call<BaseResp<List<RoomInfo>>>

    @POST("v1/room/info/ordinary")
    fun getOrdinaryRoomInfo(
        @Body detailOrdinaryReq: RoomDetailOrdinaryReq
    ): Call<BaseResp<RoomDetailOrdinary>>

    @POST("v1/room/info/periodic")
    fun getPeriodicRoomInfo(
        @Body detailOrdinaryReq: RoomDetailPeriodicReq
    ): Call<BaseResp<RoomDetailPeriodic>>

    @POST("v1/room/info/periodic-sub-room")
    fun getPeriodicSubRoomInfo(
        @Body periodicSubRoomReq: PeriodicSubRoomReq
    ): Call<BaseResp<PeriodicSubRoom>>
}