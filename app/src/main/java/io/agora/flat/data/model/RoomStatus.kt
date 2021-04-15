package io.agora.flat.data.model

enum class RoomStatus {
    // 未上课
    Idle,

    // 上课状态
    Started,

    // 暂停上课
    Paused,

    // 结束上课
    Stopped,
}