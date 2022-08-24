package io.agora.flat.common.board

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.herewhite.sdk.SyncedStore
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.board.fast.FastRoom
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.SyncedClassState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@ActivityRetainedScoped
class WhiteSyncedState @Inject constructor(
    val logger: Logger,
) : SyncedClassState {
    companion object {
        const val DEVICE_STATE_STORAGE = "deviceState"
        const val CLASSROOM_STORAGE = "classroom"
        const val ONSTAGE_USERS_STORAGE = "onStageUsers"

        const val KEY_RAISE_HAND_USERS = "raiseHandUsers"
        const val KEY_BAN = "ban"
    }

    private lateinit var syncedStore: SyncedStore
    private val gson = Gson()
    private val gsonWithNull = GsonBuilder()
        .serializeNulls()
        .create()

    private var _devicesFlow = MutableStateFlow<Map<String, DeviceState>?>(null)
    private var _onStagesFlow = MutableStateFlow<Map<String, Boolean>?>(null)
    private var _classroomStateFlow = MutableStateFlow<ClassroomStorageState?>(null)

    private var inited = false

    fun resetRoom(fastRoom: FastRoom) {
        if (inited) {
            clean()
        }
        syncedStore = fastRoom.room.syncedStore
        syncedStore.connectStorage(DEVICE_STATE_STORAGE, "{}", object : Promise<String> {
            override fun then(state: String) {
                logger.d("[deviceState] initial state: $state")
                _devicesFlow.value = getDevicesStates(state)
            }

            override fun catchEx(t: SDKError) {
            }
        })
        syncedStore.addOnStateChangedListener(DEVICE_STATE_STORAGE) { value, diff ->
            logger.d("[deviceState] updated: value: $value diff: $diff")
            _devicesFlow.value = getDevicesStates(value)
        }

        syncedStore.connectStorage(CLASSROOM_STORAGE, gson.toJson(ClassroomStorageState()), object : Promise<String> {
            override fun then(value: String) {
                logger.d("[classroom] initial state: $value")
                val state = gson.fromJson(value, ClassroomStorageState::class.java)
                _classroomStateFlow.value = ClassroomStorageState(
                    raiseHandUsers = state.raiseHandUsers,
                    ban = state.ban,
                )
            }

            override fun catchEx(t: SDKError) {
            }
        })
        syncedStore.addOnStateChangedListener(CLASSROOM_STORAGE) { value, diff ->
            logger.d("[classroom] updated: value: $value diff: $diff")
            val state = gson.fromJson(value, ClassroomStorageState::class.java)
            _classroomStateFlow.value = ClassroomStorageState(
                raiseHandUsers = state.raiseHandUsers,
                ban = state.ban,
            )
        }

        syncedStore.connectStorage(ONSTAGE_USERS_STORAGE, "{}", object : Promise<String> {
            override fun then(state: String) {
                logger.d("[onStageUsers] initial state: $state")
                _onStagesFlow.value = getOnStageUsers(state)
            }

            override fun catchEx(t: SDKError) {
            }
        })
        syncedStore.addOnStateChangedListener(ONSTAGE_USERS_STORAGE) { value, diff ->
            logger.d("[onStageUsers] updated: value: $value diff: $diff")
            _onStagesFlow.value = getOnStageUsers(value)
        }

        inited = true
    }


    private fun getDevicesStates(state: String): Map<String, DeviceState> {
        val deviceStates = mutableMapOf<String, DeviceState>()
        try {
            val jsonObject = gson.fromJson(state, JsonObject::class.java)
            jsonObject.entrySet().forEach {
                if (!it.value.isJsonNull) {
                    deviceStates[it.key] = gson.fromJson(it.value, DeviceState::class.java)
                }
            }
        } catch (e: Exception) {
            logger.e(e, "devices states parse error")
        }
        return deviceStates
    }

    private fun getOnStageUsers(state: String): Map<String, Boolean> {
        val onStageUsers = mutableMapOf<String, Boolean>()
        try {
            val jsonObject = gson.fromJson(state, JsonObject::class.java)
            jsonObject.entrySet().forEach {
                if (!it.value.isJsonNull) {
                    onStageUsers[it.key] = it.value.asBoolean
                }
            }
        } catch (e: Exception) {
            logger.e(e, "onstage users parse error")
        }
        return onStageUsers
    }

    override fun observeDeviceState(): Flow<Map<String, DeviceState>> {
        return _devicesFlow.asStateFlow().filterNotNull()
    }

    override fun observeOnStage(): Flow<Map<String, Boolean>> {
        return _onStagesFlow.asStateFlow().filterNotNull()
    }

    override fun observeClassroomState(): Flow<ClassroomStorageState> {
        return _classroomStateFlow.asStateFlow().filterNotNull()
    }

    private fun clean() {
        if (!inited) return
        _devicesFlow.value = null
        _onStagesFlow.value = null
        _classroomStateFlow.value = null
        syncedStore.disconnectStorage(DEVICE_STATE_STORAGE)
        syncedStore.disconnectStorage(ONSTAGE_USERS_STORAGE)
        syncedStore.disconnectStorage(CLASSROOM_STORAGE)
        inited = false
    }

    override fun updateDeviceState(userId: String, camera: Boolean, mic: Boolean) {
        val devicesState = mapOf(userId to DeviceState(camera, mic))
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gson.toJson(devicesState))
    }

    override fun deleteDeviceState(userId: String) {
        val devicesState = mapOf(userId to null)
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gsonWithNull.toJson(devicesState))
    }

    override fun updateOnStage(userId: String, onStage: Boolean) {
        val jsonObj = mapOf(userId to if (onStage) onStage else null)
        syncedStore.setStorageState(ONSTAGE_USERS_STORAGE, gsonWithNull.toJson(jsonObj))
    }

    override fun updateRaiseHand(userId: String, raiseHand: Boolean) {
        val raiseHandUsers = _classroomStateFlow.value?.raiseHandUsers
        val users = raiseHandUsers?.toMutableSet() ?: mutableSetOf()
        if (raiseHand) {
            users.add(userId)
        } else {
            users.remove(userId)
        }
        val jsonObj = mapOf(KEY_RAISE_HAND_USERS to users.toList())
        syncedStore.setStorageState(CLASSROOM_STORAGE, gson.toJson(jsonObj))
    }

    override fun updateBan(ban: Boolean) {
        val jsonObj = mapOf(KEY_BAN to ban)
        syncedStore.setStorageState(CLASSROOM_STORAGE, gson.toJson(jsonObj))
    }
}