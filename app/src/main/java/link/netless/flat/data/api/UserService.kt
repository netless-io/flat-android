package link.netless.flat.data.api

import link.netless.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {

    @POST("v1/login")
    fun loginCheck(
        @Body empty: BaseReq = BaseReq.EMPTY
    ): Call<BaseResp<UserInfo>>

    @POST("v1//login/weChat/set-auth-id")
    fun loginWeChatSetAuthId(
        @Body req: WeChatSetAuthIdReq
    ): Call<BaseResp<RespNoData>>

    @GET("v1/login/weChat/mobile/callback")
    fun loginWeChatCallback(
        @Query("state") state: String,
        @Query("code") code: String,
    ): Call<BaseResp<UserInfoWithToken>>
}