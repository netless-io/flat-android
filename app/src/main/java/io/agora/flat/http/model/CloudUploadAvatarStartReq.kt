package io.agora.flat.http.model

data class CloudUploadAvatarStartReq constructor(
    val fileName: String,
    val fileSize: Long,
)