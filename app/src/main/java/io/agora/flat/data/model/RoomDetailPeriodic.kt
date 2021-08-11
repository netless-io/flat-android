package io.agora.flat.data.model

/**
 * 周期性房间详情
 */
data class RoomDetailPeriodic constructor(
    val periodic: RoomPeriodic,
    val rooms: ArrayList<RoomInfo>,
)
