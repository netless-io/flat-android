package link.netless.flat.data.api

import link.netless.flat.data.model.BaseReq
import link.netless.flat.data.model.BaseResp
import link.netless.flat.data.model.UserInfo
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST("v1/login")
    suspend fun getUserInfo(@Body empty: BaseReq = BaseReq.EMPTY): BaseResp<UserInfo>
}