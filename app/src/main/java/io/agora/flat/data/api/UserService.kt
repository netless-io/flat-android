package io.agora.flat.data.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {

    @POST("v1/login")
    fun loginCheck(
        @Body req: LoginCheckReq = LoginCheckReq()
    ): Call<BaseResp<UserInfo>>

    @POST("v1/login/set-auth-uuid")
    fun loginSetAuthUUID(
        @Body req: AuthUUIDReq
    ): Call<BaseResp<RespNoData>>

    @GET("v1/login/weChat/mobile/callback")
    fun loginWeChatCallback(
        @Query("state") state: String,
        @Query("code") code: String,
    ): Call<BaseResp<UserInfoWithToken>>

    @POST("v1/login/process")
    fun loginProcess(
        @Body req: AuthUUIDReq
    ): Call<BaseResp<UserInfoWithToken>>
}