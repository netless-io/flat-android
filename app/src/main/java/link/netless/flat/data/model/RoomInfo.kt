package link.netless.flat.data.model

/**
 * 房间列表信息项
 */
data class RoomInfo(
    // 房间的 uuid
    val roomUUID: String,
    // 房间类型
    val roomType: RoomType,
    // 周期性房间的 uuid
    val periodicUUID: String? = null,
    // 房间所有者的名称
    val ownerUUID: String,
    // 房间所有者的名称
    val ownerName: String,
    // 房间标题
    val title: String,
    // 房间开始时间
    val beginTime: Long,
    // 结束时间
    val endTime: Long,
    // 房间状态
    val roomStatus: RoomStatus,
    // 是否存在录制(只有历史记录才会有)
    val hasRecord: Boolean,
) {
    // local for listHead
    var showDayHead: Boolean = false
}
