package io.agora.flat.http.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {

    @POST("v1/login")
    fun loginCheck(
        @Body req: LoginCheckReq = LoginCheckReq(),
    ): Call<BaseResp<UserInfo>>

    @POST("v1/login/set-auth-uuid")
    fun loginSetAuthUUID(
        @Body req: AuthUUIDReq,
    ): Call<BaseResp<RespNoData>>

    @GET("v1/login/weChat/mobile/callback")
    fun loginWeChatCallback(
        @Query("state") state: String,
        @Query("code") code: String,
    ): Call<BaseResp<UserInfoWithToken>>

    @POST("v1/login/process")
    fun loginProcess(
        @Body req: AuthUUIDReq,
    ): Call<BaseResp<UserInfoWithToken>>

    @POST("v1/login/phone/sendMessage")
    fun requestSmsCode(
        @Body req: PhoneReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/login/phone")
    fun loginWithPhone(
        @Body req: PhoneSmsCodeReq,
    ): Call<BaseResp<UserInfoWithToken>>

    @POST("v1/binding/platform/phone/sendMessage")
    fun requestBindSmsCode(
        @Body req: PhoneReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/binding/platform/phone")
    fun bindPhone(
        @Body req: PhoneSmsCodeReq,
    ): Call<BaseResp<RespNoData>>

    /**
     * [UserRenameReq.name] length limited as 1..50
     */
    @POST("v1/user/rename")
    fun rename(
        @Body req: UserRenameReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/deleteAccount/validate")
    fun validateDeleteAccount(
        @Body req: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<RoomCount>>

    @POST("v1/user/deleteAccount")
    fun deleteAccount(
        @Body req: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/binding/list")
    fun listBindings(
        @Body req: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<UserBindings>>

    @POST("v1/user/binding/set-auth-uuid")
    fun bindingSetAuthUUID(
        @Body req: AuthUUIDReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/binding/process")
    fun bindingProcess(
        @Body req: AuthUUIDReq,
    ): Call<BaseResp<UserInfoWithToken>>

    @GET("v1/user/binding/platform/wechat/mobile")
    fun bindWeChat(
        @Query("state") state: String,
        @Query("code") code: String,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/binding/remove")
    fun removeBinding(
        @Body req: RemoveBindingReq,
    ): Call<BaseResp<UserTokenData>>
}