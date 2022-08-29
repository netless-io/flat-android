package io.agora.flat.data.repository

import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Result
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.toResult
import io.agora.flat.http.api.CloudStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStorageRepository @Inject constructor(
    private val cloudStorageService: CloudStorageService,
    private val appKVCenter: AppKVCenter,
) {
    suspend fun listFiles(
        page: Int = 1,
        size: Int = 50,
        order: String = "DESC",
    ): Result<CloudStorageFileListResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.listFiles(page, size, order).toResult()
        }
    }

    suspend fun updateStart(
        fileName: String,
        fileSize: Long,
        region: String = "cn-hz",
    ): Result<CloudStorageUploadStartResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.updateStart(
                CloudStorageUploadStartReq(fileName, fileSize, region)
            ).toResult()
        }
    }

    suspend fun updateFinish(
        fileUUID: String,
        region: String = "cn-hz",
        projector: Boolean? = null,
    ): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.updateFinish(
                CloudStorageFileReq(fileUUID, region, projector)
            ).toResult()
        }
    }

    suspend fun remove(fileUUIDs: List<String>): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.remove(CloudStorageRemoveReq(fileUUIDs)).toResult()
        }
    }

    suspend fun cancel(fileUUIDs: List<String> = listOf()): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.cancel().toResult()
        }
    }

    suspend fun convertStart(
        fileUUID: String,
        region: String = "cn-hz",
        projector: Boolean? = null,
    ): Result<CloudStorageFileConvertResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.convertStart(
                CloudStorageFileReq(fileUUID, region, projector)
            ).toResult()
        }
    }

    suspend fun convertFinish(
        fileUUID: String,
        region: String = "cn-hz",
    ): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.convertFinish(
                CloudStorageFileReq(fileUUID, region, null)
            ).toResult()
        }
    }

    suspend fun updateAvatarStart(
        fileName: String,
        fileSize: Long,
        region: String = "cn-hz",
    ): Result<CloudStorageUploadStartResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.updateAvatarStart(
                CloudStorageUploadStartReq(fileName, fileSize, region)
            ).toResult()
        }
    }

    suspend fun updateAvatarFinish(
        fileUUID: String,
        region: String = "cn-hz",
    ): Result<AvatarData> {
        return withContext(Dispatchers.IO) {
            val result = cloudStorageService.updateAvatarFinish(
                CloudStorageFileReq(
                    fileUUID,
                    region,
                    null
                )
            ).toResult()
            // update local avatar.
            // there is a doubt here that CloudRepository may update userinfo.
            if (result is Success) {
                appKVCenter.getUserInfo()?.copy(avatar = result.data.avatarURL)?.run {
                    appKVCenter.setUserInfo(this)
                }
            }
            result
        }
    }
}