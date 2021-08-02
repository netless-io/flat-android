package io.agora.flat.data.model

data class CloudStorageUploadStartResp constructor(
    val fileUUID: String,
    val filePath: String,
    val policy: String,
    val signature: String,
)