package io.agora.flat.data.api

import io.agora.flat.data.model.MessageListResp
import io.agora.flat.data.model.MessageQueryHistoryReq
import io.agora.flat.data.model.MessageQueryHistoryResp
import retrofit2.Call
import retrofit2.http.*

/**
 * 未归类接口
 */
interface MessageService {
    @POST("v2/project/{appId}/rtm/message/history/query")
    fun queryHistory(
        @Path("appId") appId: String,
        @Body req: MessageQueryHistoryReq,
        @Header("x-agora-uid") agoraUid: String,
        @Header("x-agora-token") agoraToken: String,
    ): Call<MessageQueryHistoryResp>

    @GET("v2/project/{appId}/rtm/message/history/query/{handle}")
    fun getMessageList(
        @Path("appId") appId: String,
        @Path("handle") handle: String,
        @Header("x-agora-uid") agoraUid: String,
        @Header("x-agora-token") agoraToken: String,
    ): Call<MessageListResp>
}