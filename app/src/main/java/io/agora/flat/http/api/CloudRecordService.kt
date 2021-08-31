package io.agora.flat.http.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudRecordService {
    // 开始录制
    @POST("v1/room/record/started")
    fun startRecord(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>

    // 结束录制
    @POST("v1/room/record/stopped")
    fun stopRecord(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>

    // 获取录制结果信息
    @POST("v1/room/record/info")
    fun getRecordInfo(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RecordInfo>>

    // 更新录制时间
    @POST("v1/room/record/update-end-time")
    fun updateRecordEndTime(
        @Body req: PureRoomReq,
    ): Call<BaseResp<RespNoData>>

    // 获取云端录制资源
    @POST("v1/room/record/agora/acquire")
    fun acquireRecord(
        @Body req: RecordAcquireReq,
    ): Call<BaseResp<RecordAcquireRespData>>

    // 开始录制(包含agora的音视频)
    @POST("v1/room/record/agora/started")
    fun startRecordWithAgora(
        @Body req: RecordStartReq,
    ): Call<BaseResp<RecordStartRespData>>

    // 取查询云端录制状态(包含agora的音视频)
    @POST("v1/room/record/agora/query")
    fun queryRecordWithAgora(
        @Body req: RecordReq,
    ): Call<BaseResp<RecordQueryRespData>>

    // 录制时更新合流布局(包含agora的音视频)
    @POST("v1/room/record/agora/update-layout")
    fun updateRecordLayoutWithAgora(
        @Body req: RecordUpdateLayoutReq,
    ): Call<BaseResp<RecordQueryRespData>>

    // 结束录制(包含agora的音视频)
    @POST("v1/room/record/agora/stopped")
    fun stopRecordWithAgora(
        @Body req: RecordReq,
    ): Call<BaseResp<RecordStopRespData>>
}