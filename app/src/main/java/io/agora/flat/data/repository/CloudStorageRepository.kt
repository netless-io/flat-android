package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.api.CloudStorageService
import io.agora.flat.data.executeOnce
import io.agora.flat.data.model.CloudStorageFileListResp
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
            cloudStorageService.getFileList(page).executeOnce().toResult()
        }
    }
}