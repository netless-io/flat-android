package io.agora.flat.common.board

data class DeviceState(
    val camera: Boolean,
    val mic: Boolean,
)

data class ClassroomState(
    val raiseHandUsers: List<String> = listOf(),
    val ban: Boolean = false
)

data class UserWindows(
    val grid: List<String> = listOf(),
    val users: Map<String, WindowInfo> = mapOf()
)

data class WindowInfo(
    val x: Float,
    val y: Float,
    val z: Int,
    val width: Float,
    val height: Float,
)