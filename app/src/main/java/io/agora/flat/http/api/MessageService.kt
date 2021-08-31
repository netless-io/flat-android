package io.agora.flat.http.api

import io.agora.flat.data.model.MessageCountResp
import io.agora.flat.data.model.MessageListResp
import io.agora.flat.data.model.MessageQueryHistoryReq
import io.agora.flat.data.model.MessageQueryHistoryResp
import retrofit2.Call
import retrofit2.http.*

/**
 * https://docs.agora.io/cn/Real-time-Messaging/rtm_get_event?platform=RESTful
 */
interface MessageService {
    /**
     * 创建历史消息查询资源 API
     */
    @POST("v2/project/{appId}/rtm/message/history/query")
    fun queryHistory(
        @Path("appId") appId: String,
        @Body req: MessageQueryHistoryReq,
        @Header("x-agora-uid") agoraUid: String,
        @Header("x-agora-token") agoraToken: String,
    ): Call<MessageQueryHistoryResp>

    /**
     * 获取历史消息 API
     */
    @GET("v2/project/{appId}/rtm/message/history/query/{handle}")
    fun getMessageList(
        @Path("appId") appId: String,
        @Path("handle") handle: String,
        @Header("x-agora-uid") agoraUid: String,
        @Header("x-agora-token") agoraToken: String,
    ): Call<MessageListResp>


    @GET("v2/project/{appId}/rtm/message/history/count")
    fun getMessageCount(
        @Path("appId") appId: String,
        @Query("source") source: String? = null,
        @Query("destination") destination: String? = null,
        @Query("start_time") startTime: String,
        @Query("end_time") endTime: String,
        @Header("x-agora-uid") agoraUid: String,
        @Header("x-agora-token") agoraToken: String,
    ): Call<MessageCountResp>
}