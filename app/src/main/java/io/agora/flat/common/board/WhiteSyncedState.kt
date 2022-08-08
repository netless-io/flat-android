package io.agora.flat.common.board

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.herewhite.sdk.SyncedStore
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import io.agora.board.fast.FastRoom
import io.agora.flat.data.model.ClassModeType
import io.agora.flat.di.interfaces.SyncedClassState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhiteSyncedState @Inject constructor() : SyncedClassState {
    companion object {
        const val TAG = "WhiteSyncedState"
        const val DEVICE_STATE_STORAGE = "deviceState"
        const val CLASSROOM_STORAGE = "classroom"
    }

    private lateinit var fastRoom: FastRoom
    private lateinit var syncedStore: SyncedStore

    private val gson = Gson()
    private val gsonWithNull = GsonBuilder()
        .serializeNulls()
        .create()

    private var deviceStates = mutableMapOf<String, DeviceState>()
    // private var onStageStates = mutableListOf<String>()
    // private var raiseHandsStates = mutableListOf<String>()

    // private var classRoomState: ClassroomStorageState = ClassroomStorageState()

    private var _devicesFlow = MutableStateFlow<Map<String, DeviceState>?>(null)
    private var _onStagesFlow = MutableStateFlow<List<String>?>(null)
    private var _raiseHandsFlow = MutableStateFlow<List<String>?>(null)

    fun resetRoom(fastRoom: FastRoom) {
        this.fastRoom = fastRoom
        syncedStore = fastRoom.room.syncedStore
        syncedStore.connectStorage(DEVICE_STATE_STORAGE, "{}", object : Promise<String> {
            override fun then(state: String) {
                Log.d(TAG, "[deviceState] initial state: $state")
                deviceStates = getDevicesStates(state)
                _devicesFlow.value = deviceStates.toMap()
            }

            override fun catchEx(t: SDKError) {
            }
        })
        syncedStore.connectStorage(CLASSROOM_STORAGE, gson.toJson(ClassroomStorageState()), object : Promise<String> {
            override fun then(state: String) {
                Log.d(TAG, "[deviceState] initial state: $state")
                val classRoomState = gson.fromJson(state, ClassroomStorageState::class.java)
                // onStageStates.addAll(classRoomState.onStageUsers ?: listOf())
                // raiseHandsStates.addAll(classRoomState.raiseHandUsers ?: listOf())

                _onStagesFlow.value = classRoomState.onStageUsers ?: listOf()
                _raiseHandsFlow.value = classRoomState.raiseHandUsers ?: listOf()
            }

            override fun catchEx(t: SDKError) {
            }
        })

        syncedStore.addOnStateChangedListener(DEVICE_STATE_STORAGE) { value, diff ->
            Log.d(TAG, "[deviceState] updated: value: $value\ndiff: $diff")
            val states = getDevicesStates(diff)
            _devicesFlow.value = (deviceStates + states).toMap()
        }

        syncedStore.addOnStateChangedListener(CLASSROOM_STORAGE) { value, diff ->
            Log.d(TAG, "[classroom] updated: value: $value\ndiff: $diff")
            val state = gson.fromJson(diff, ClassroomStorageState::class.java)

            state.classMode?.let {

            }
            state.raiseHandUsers?.let {
                _raiseHandsFlow.value = it
            }
            state.onStageUsers?.let {
                _onStagesFlow.value = it
            }
        }
    }

    private fun getDevicesStates(state: String): MutableMap<String, DeviceState> {
        val deviceStates = mutableMapOf<String, DeviceState>()
        try {
            val jsonObject = gson.fromJson(state, JsonObject::class.java)
            val entrySet = jsonObject.entrySet()
            entrySet.forEach {
                deviceStates[it.key] = gson.fromJson(it.value, DeviceState::class.java)
            }
        } catch (e: Exception) {
        }
        return deviceStates
    }

    override fun observeDeviceState(): Flow<Map<String, DeviceState>> {
        return _devicesFlow.asStateFlow().filterNotNull()
    }

    override fun observeOnStage(): Flow<List<String>> {
        return _onStagesFlow.asStateFlow().filterNotNull()
    }

    override fun observeRaiseHand(): Flow<List<String>> {
        return _raiseHandsFlow.asStateFlow().filterNotNull()
    }

    override fun updateDeviceState(userId: String, camera: Boolean, mic: Boolean) {
        val devicesState = mapOf(userId to DeviceState(camera, mic))
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gson.toJson(devicesState))
    }

    override fun deleteDeviceState(userId: String) {
        val devicesState = mapOf(userId to null)
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gson.toJson(devicesState))
    }

    override fun updateOnStage(userId: String, onStage: Boolean) {
        val onStageUsers = _onStagesFlow.value?.toMutableSet() ?: mutableSetOf()
        if (onStage) {
            onStageUsers.add(userId)
        } else {
            onStageUsers.remove(userId)
        }
        val jsonObj = mapOf("onStageUsers" to onStageUsers.toList())
        syncedStore.setStorageState(CLASSROOM_STORAGE, gson.toJson(jsonObj))
    }

    override fun updateRaiseHand(userId: String, raiseHand: Boolean) {
        val usersSet = _raiseHandsFlow.value?.toMutableSet() ?: mutableSetOf()
        if (raiseHand) {
            usersSet.add(userId)
        } else {
            usersSet.remove(userId)
        }
        val jsonObj = mapOf("raiseHandUsers" to usersSet.toList())
        syncedStore.setStorageState(CLASSROOM_STORAGE, gson.toJson(jsonObj))
    }

    override fun updateClassModeType(classModeType: ClassModeType) {

    }
}