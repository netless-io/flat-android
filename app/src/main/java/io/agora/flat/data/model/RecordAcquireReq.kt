package io.agora.flat.data.model

data class RecordAcquireReq constructor(
    val roomUUID: String,
    val agoraData: RecordAcquireReqData,
)

data class RecordAcquireReqData constructor(
    val clientRequest: RecordAcquireReqDataClientRequest,
)

data class RecordAcquireReqDataClientRequest(
    val resourceExpiredHour: Int,
    val scene: Int,
)