package io.agora.flat.http.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface RoomService {
    @POST("v1/room/list/all")
    fun getRoomAll(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<List<RoomInfo>>>

    @POST("v1/room/list/history")
    fun getRoomHistory(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<List<RoomInfo>>>

    @POST("v1/room/info/ordinary")
    fun getOrdinaryRoomInfo(
        @Body detailOrdinaryReq: RoomDetailOrdinaryReq,
    ): Call<BaseResp<RoomDetailOrdinary>>

    @POST("v1/room/info/periodic")
    fun getPeriodicRoomInfo(
        @Body detailOrdinaryReq: RoomDetailPeriodicReq,
    ): Call<BaseResp<RoomDetailPeriodic>>

    @POST("v1/room/info/periodic-sub-room")
    fun getPeriodicSubRoomInfo(
        @Body periodicSubRoomReq: PeriodicSubRoomReq,
    ): Call<BaseResp<PeriodicSubRoom>>

    // 取消房间
    @POST("v1/room/cancel/ordinary")
    fun cancelOrdinary(
        @Body cancelRoomReq: CancelRoomReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/room/cancel/periodic")
    fun cancelPeriodic(
        @Body cancelRoomReq: CancelRoomReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/room/cancel/periodic-sub-room")
    fun cancelPeriodicSubRoom(
        @Body cancelRoomReq: CancelRoomReq,
    ): Call<BaseResp<RespNoData>>

    // 加入房间
    @POST("v1/room/join")
    fun joinRoom(
        @Body joinRoomReq: JoinRoomReq,
    ): Call<BaseResp<RoomPlayInfo>>

    // 获取房间内所有用户的信息
    @POST("v1/room/info/users")
    fun getRoomUsers(
        @Body roomUsersReq: RoomUsersReq,
    ): Call<BaseResp<Map<String, NetworkRoomUser>>>

    // 创建房间
    @POST("v1/room/create/ordinary")
    fun createOrdinary(
        @Body romCreateReq: RoomCreateReq,
    ): Call<BaseResp<RoomCreateRespData>>

    // 上课接口
    @POST("v1/room/update-status/started")
    fun startRoomClass(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/room/update-status/paused")
    fun pauseRoomClass(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/room/update-status/stopped")
    fun stopRoomClass(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>
}