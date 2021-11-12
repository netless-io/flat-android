package io.agora.flat.ui.activity.home.mainext

data class MainExtState(
    val type: ExtPageType = ExtPageType.Empty,
)

enum class ExtPageType {
    Empty,
    RoomCreate,
    RoomJoin,
    Setting,
    RoomDetail,
}