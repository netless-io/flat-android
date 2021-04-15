package io.agora.flat.data.model

data class PeriodicSubRoomReq(
    val periodicUUID: String,
    val roomUUID: String,
    val needOtherRoomTimeInfo: Boolean?,
)
