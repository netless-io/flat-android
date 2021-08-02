package io.agora.flat.data.model

data class CloudStorageRenameReq constructor(
    val fileUUID: String,
    val fileName: String,
)