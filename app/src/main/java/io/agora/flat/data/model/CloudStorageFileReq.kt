package io.agora.flat.data.model

data class CloudStorageFileReq constructor(
    val fileUUID: String,
    val region: String,
    val isWhiteboardProjector: Boolean?,
)