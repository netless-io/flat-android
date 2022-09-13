package io.agora.flat.http.model

data class CreateDirectoryReq(
    val parentDirectoryPath: String,
    val directoryName: String,
)