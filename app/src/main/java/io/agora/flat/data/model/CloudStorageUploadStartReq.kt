package io.agora.flat.data.model

data class CloudStorageUploadStartReq constructor(
    val fileName: String,
    val fileSize: Long,
    val region: String,
)