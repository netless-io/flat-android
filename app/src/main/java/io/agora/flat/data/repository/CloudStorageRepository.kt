package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.api.CloudStorageService
import io.agora.flat.data.model.*
import io.agora.flat.data.toResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStorageRepository @Inject constructor(
    private val cloudStorageService: CloudStorageService,
) {
    suspend fun getFileList(page: Int): Result<CloudStorageFileListResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.getFileList(page).toResult()
        }
    }

    suspend fun updateStart(
        fileName: String,
        fileSize: Long,
        region: String = "cn-hz",
    ): Result<CloudStorageUploadStartResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.updateStart(CloudStorageUploadStartReq(fileName, fileSize, region)).toResult()
        }
    }

    suspend fun updateFinish(fileUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.updateFinish(CloudStorageFileReq(fileUUID)).toResult()
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

    suspend fun convertStart(fileUUID: String): Result<CloudStorageFileConvertResp> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.convertStart(CloudStorageFileReq(fileUUID)).toResult()
        }
    }

    suspend fun convertFinish(fileUUID: String): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            cloudStorageService.convertFinish(CloudStorageFileReq(fileUUID))
                .toResult()
        }
    }
}