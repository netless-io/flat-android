package io.agora.flat.ui.activity.cloud.list

import io.agora.flat.common.upload.UploadFile
import io.agora.flat.data.model.CLOUD_ROOT_DIR
import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.LoadState

data class CloudUiFile(
    val file: CloudFile,
    val checked: Boolean = false,
)

data class CloudStorageUiState(
    val loadUiState: LoadUiState = LoadUiState.Init,
    // val refreshing: Boolean = false,
    val showBadge: Boolean = false,
    val totalUsage: Long = 0,
    val files: List<CloudUiFile> = emptyList(),
    val uploadFiles: List<UploadFile> = emptyList(),
    val dirPath: String = CLOUD_ROOT_DIR,
    val errorMessage: String? = null,
) {
    val deletable: Boolean
        get() = files.any { it.checked }
}

data class LoadUiState(
    val page: Int,
    val refresh: LoadState,
    val append: LoadState,
) {
    companion object {
        val Init = LoadUiState(
            page = 1,
            LoadState.NotLoading(false),
            LoadState.NotLoading(false)
        )
    }
}