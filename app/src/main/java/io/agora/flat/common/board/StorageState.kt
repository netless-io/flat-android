package io.agora.flat.common.board

data class DeviceState(
    val camera: Boolean,
    val mic: Boolean,
)

data class ClassroomState(
    val raiseHandUsers: List<String> = listOf(),
    val ban: Boolean = false
)