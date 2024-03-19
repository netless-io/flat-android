package io.agora.flat.data.model

data class RecordInfo constructor(
    val title: String,
    val ownerUUID: String,
    val roomType: RoomType,
    val region: String,
    val whiteboardRoomToken: String,
    val whiteboardRoomUUID: String,
    val rtmToken: String,
    val recordInfo: List<RecordItem>,
)

data class RecordItem constructor(
    val beginTime: Long,
    val endTime: Long,
    val videoURL: String,
)