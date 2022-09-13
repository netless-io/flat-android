package io.agora.flat.ui.activity.cloud.list

import io.agora.flat.common.upload.UploadFile
import io.agora.flat.data.model.CloudFile

data class CloudUiFile(
    val file: CloudFile,
    val checked: Boolean = false,
)

data class CloudStorageUiState(
    val refreshing: Boolean = false,
    val showBadge: Boolean = false,
    val totalUsage: Long = 0,
    val files: List<CloudUiFile> = emptyList(),
    val uploadFiles: List<UploadFile> = emptyList(),
    val dirPath: String = "/",
    val errorMessage: String? = null,
) {
    val deletable: Boolean
        get() = files.any { it.checked }
}