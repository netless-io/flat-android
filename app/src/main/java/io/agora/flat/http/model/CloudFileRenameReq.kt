package io.agora.flat.http.model

data class CloudFileRenameReq(
    val fileUUID: String,
    val newName: String,
)
