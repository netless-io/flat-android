package io.agora.flat.di.interfaces

import io.agora.flat.common.board.ClassroomState
import io.agora.flat.common.board.DeviceState
import io.agora.flat.common.board.UserWindows
import io.agora.flat.common.board.WindowInfo
import kotlinx.coroutines.flow.Flow

/**
 * class room global state
 */
interface SyncedClassState {
    fun observeSyncedReady(): Flow<Boolean>

    fun observeDeviceState(): Flow<Map<String, DeviceState>>

    fun observeOnStage(): Flow<Map<String, Boolean>>

    fun observeWhiteboard(): Flow<Map<String, Boolean>>

    fun observeClassroomState(): Flow<ClassroomState>

    fun observeUserWindows(): Flow<UserWindows>

    /**
     * 更新设备状态
     */
    fun updateDeviceState(userId: String, camera: Boolean, mic: Boolean)

    /**
     * 删除设备状态
     */
    fun deleteDeviceState(userId: String)

    /**
     * 静音用户麦克风
     */
    fun muteDevicesMic(userIds: List<String>)

    /**
     * 更新上台状态
     */
    fun updateOnStage(userId: String, onStage: Boolean)

    /**
     * 所有学生下台
     */
    fun stageOffAll()

    /**
     * 更新白板权限
     */
    fun updateWhiteboard(userId: String, allowDraw: Boolean)

    /**
     * 更新举手状态
     */
    fun updateRaiseHand(userId: String, raiseHand: Boolean)

    /**
     * 更新禁言状态
     */
    fun updateBan(ban: Boolean)

    fun maximizeWindows(userId: String)

    fun removeMaximizeWindow(userId: String)

    fun normalizeWindows()

    fun updateNormalWindow(userId: String, window: WindowInfo)

    fun removeNormalWindow(userId: String)

    fun removeAllWindow()
}