package io.agora.flat.di.interfaces

import io.agora.flat.common.board.ClassroomState
import io.agora.flat.common.board.DeviceState
import kotlinx.coroutines.flow.Flow

/**
 * class room global state
 */
interface SyncedClassState {
    fun observeDeviceState(): Flow<Map<String, DeviceState>>

    fun observeOnStage(): Flow<Map<String, Boolean>>

    fun observeClassroomState(): Flow<ClassroomState>

    /**
     * 更新设备状态
     */
    fun updateDeviceState(userId: String, camera: Boolean, mic: Boolean)

    /**
     * 删除设备状态
     */
    fun deleteDeviceState(userId: String)

    /**
     * 更新上台状态
     */
    fun updateOnStage(userId: String, onStage: Boolean)

    /**
     * 更新举手状态
     */
    fun updateRaiseHand(userId: String, raiseHand: Boolean)

    /**
     * 更新禁言状态
     */
    fun updateBan(ban: Boolean)
}