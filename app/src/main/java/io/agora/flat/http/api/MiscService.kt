package io.agora.flat.http.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 未归类接口
 */
interface MiscService {
    // 开始录制
    @POST("v1/agora/token/generate/rtc")
    fun generateRtcToken(
        @Body req: PureRoomReq,
    ): Call<BaseResp<PureToken>>

    // 结束录制
    @POST("v1/agora/token/generate/rtm")
    fun generateRtmToken(
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<PureToken>>

    // @POST("v1/log")
    // fun logError(
    //     @Body req: LogErrorReq,
    // ): Call<BaseResp<RespNoData>>

    // 消息审核
    @POST("v1/agora/rtm/censor")
    fun censorRtm(
        @Body req: RtmCensorReq,
    ): Call<BaseResp<RtmCensorRespData>>

    @GET("v2/region/configs")
    fun getRegionConfigs(): Call<BaseResp<RegionConfigs>>

    @POST("v1/user/agreement/get")
    fun getStreamAgreement(
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<StreamAgreement>>

    @POST("v1/user/agreement/set")
    fun setStreamAgreement(
        @Body req: StreamAgreementReq,
    ): Call<BaseResp<RespNoData>>
}