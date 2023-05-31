package io.agora.flat.http.model

data class CloudUploadTempFileStartReq constructor(
    val fileName: String,
    val fileSize: Long,
)