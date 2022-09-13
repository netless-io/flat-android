package io.agora.flat.http.model

data class CloudFileMoveReq(
    val uuids: List<String>,
    val targetDirectoryPath: String,
)
