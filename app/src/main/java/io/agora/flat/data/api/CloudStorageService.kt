package io.agora.flat.data.api

import io.agora.flat.data.model.BaseReq
import io.agora.flat.data.model.BaseResp
import io.agora.flat.data.model.CloudStorageFileListResp
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudStorageService {
    @POST("v1/cloud-storage/list")
    fun getFileList(
        @Query(value = "page") page: Int = 1,
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<CloudStorageFileListResp>>
}