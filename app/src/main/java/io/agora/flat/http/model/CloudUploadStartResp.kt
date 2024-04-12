package io.agora.flat.http.model

data class CloudUploadStartResp constructor(
    val fileUUID: String,
    val ossDomain: String,
    val ossFilePath: String,
    val policy: String,
    val signature: String,
    val convertType: String? = null,
)