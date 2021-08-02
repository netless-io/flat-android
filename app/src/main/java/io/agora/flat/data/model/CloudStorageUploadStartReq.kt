package io.agora.flat.data.model

data class CloudStorageUploadStartReq constructor(
    val fileName: String,
    val fileSize: Int,
    val region: String = "cn-hz",
)