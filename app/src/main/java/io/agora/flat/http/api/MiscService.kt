package io.agora.flat.http.api

import io.agora.flat.data.model.BaseReq
import io.agora.flat.data.model.BaseResp
import io.agora.flat.data.model.PureRoomReq
import io.agora.flat.data.model.PureToken
import retrofit2.Call
import retrofit2.http.Body
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
}