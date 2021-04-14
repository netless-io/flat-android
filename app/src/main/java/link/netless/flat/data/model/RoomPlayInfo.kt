package link.netless.flat.data.model

/**
 * 加入房间后获取的信息
 */
data class RoomPlayInfo(
    // 房间类型
    val roomType: RoomType,
    // 当前房间的 UUID
    val roomUUID: String,
    // 房间创建者的 UUID
    val ownerUUID: String,
    // 白板的 room token
    val whiteboardRoomToken: String,
    // 白板的 room uuid
    val whiteboardRoomUUID: String,
    // rtc 的 uid
    val rtcUID: Long,
    // rtc token
    val rtcToken: String,
    // rtm token
    val rtmToken: String,
)
