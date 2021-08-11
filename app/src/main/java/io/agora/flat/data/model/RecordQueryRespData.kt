package io.agora.flat.data.model

data class RecordQueryRespData constructor(
    val sid: String,
    val resourceId: String,
    val serverResponse: QueryServerResponse,
)

data class QueryServerResponse constructor(
    val status: Int,
    val fileList: String,
    // val fileListMode: String,
    val sliceStartTime: Long,
)