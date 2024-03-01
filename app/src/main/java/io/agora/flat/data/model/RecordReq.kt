package io.agora.flat.data.model

data class RecordReq(
    val roomUUID: String,
    val agoraParams: AgoraRecordParams,
)