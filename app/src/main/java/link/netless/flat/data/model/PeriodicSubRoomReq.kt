package link.netless.flat.data.model

data class PeriodicSubRoomReq constructor(
    val periodicUUID: String,
    val roomUUID: String,
    val needOtherRoomTimeInfo: Boolean?,
)
