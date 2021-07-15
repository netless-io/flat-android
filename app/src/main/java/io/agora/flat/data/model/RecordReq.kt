package io.agora.flat.data.model

data class RecordReq constructor(
    val roomUUID: String,
    val agoraParams: AgoraRecordParams,
)