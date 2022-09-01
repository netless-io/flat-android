package io.agora.flat.ui.activity.cloud.uploading

import io.agora.flat.common.upload.UploadFile

data class UploadingUIState(
    val uploadFiles: List<UploadFile> = emptyList(),
)