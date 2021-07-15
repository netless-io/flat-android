package io.agora.flat.data.model

data class RecordQueryRespData constructor(
    val sid: String,
    val resourceId: String,
    val serverResponse: String? = null
)