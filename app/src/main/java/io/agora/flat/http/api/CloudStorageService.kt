package io.agora.flat.http.api

import io.agora.flat.data.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudStorageService {
    @POST("v1/cloud-storage/list")
    fun listFiles(
        @Query(value = "page") page: Int,
        @Query(value = "size") size: Int,
        @Query(value = "order") order: String,
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<CloudStorageFileListResp>>

    @POST("v1/cloud-storage/alibaba-cloud/upload/start")
    fun updateStart(
        @Body req: CloudStorageUploadStartReq,
    ): Call<BaseResp<CloudStorageUploadStartResp>>

    @POST("v1/cloud-storage/alibaba-cloud/upload/finish")
    fun updateFinish(
        @Body req: CloudStorageFileReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/cloud-storage/alibaba-cloud/remove")
    fun remove(
        @Body req: CloudStorageRemoveReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/cloud-storage/alibaba-cloud/rename")
    fun rename(
        @Body req: CloudStorageRenameReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/cloud-storage/upload/cancel")
    fun cancel(
        @Body empty: BaseReq = BaseReq.EMPTY,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/cloud-storage/convert/start")
    fun convertStart(
        @Body req: CloudStorageFileReq,
    ): Call<BaseResp<CloudStorageFileConvertResp>>

    @POST("v1/cloud-storage/convert/finish")
    fun convertFinish(
        @Body req: CloudStorageFileReq,
    ): Call<BaseResp<RespNoData>>

    @POST("v1/user/upload-avatar/start")
    fun updateAvatarStart(
        @Body req: CloudStorageUploadStartReq,
    ): Call<BaseResp<CloudStorageUploadStartResp>>

    @POST("v1/user/upload-avatar/finish")
    fun updateAvatarFinish(
        @Body req: CloudStorageFileReq,
    ): Call<BaseResp<AvatarData>>

}