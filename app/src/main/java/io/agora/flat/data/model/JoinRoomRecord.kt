package io.agora.flat.data.model

data class JoinRoomRecordList(
    val items: List<JoinRoomRecord> = emptyList()
)

data class JoinRoomRecord(
    val title: String,
    val uuid: String
)
