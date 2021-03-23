package com.agora.netless.flat.data.api

import com.agora.netless.flat.data.model.BaseReq
import com.agora.netless.flat.data.model.BaseResp
import com.agora.netless.flat.data.model.UserInfo
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST("login")
    suspend fun getUserInfo(@Body empty: BaseReq = BaseReq.EMPTY): BaseResp<UserInfo>
}