package io.agora.flat.data.model

data class PeriodicSubRoom constructor(
    val roomInfo: RoomInfo,
    val previousPeriodicRoomBeginTime: Long?,
    val nextPeriodicRoomEndTime: Long?,
    val count: Int,
    val docs: List<RoomDocs>,
)
