package io.agora.flat.common.board

import io.agora.flat.data.model.ClassModeType

data class DeviceState(
    val camera: Boolean,
    val mic: Boolean,
)

data class ClassroomStorageState(
    val classMode: ClassModeType? = null,
    val raiseHandUsers: List<String>? = null,
)