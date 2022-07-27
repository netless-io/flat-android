package io.agora.flat.common.board

import io.agora.flat.data.model.ClassModeType

data class DeviceState(
    val camera: Boolean,
    val mic: Boolean,
)

class ClassroomStorageState(
    val classMode: ClassModeType? = ClassModeType.Lecture,
    val raiseHandUsers: List<String>? = listOf(),
    val onStageUsers: List<String>? = listOf(),
)