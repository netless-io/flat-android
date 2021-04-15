package io.agora.flat.data.model

data class RoomPeriodic constructor(
    // 创建者的 uuid
    val ownerUUID: String,
    // 房间类型
    val roomType: RoomType,
    // 结束时间
    val endTime: Long,
    // 为 null 时（即 用户选择的是 endTime）
    val rate: Long?,
    val title: String,
    val weeks: List<Week>,
)