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
        const val WHITEBOARD_STORAGE = "whiteboard"
        const val USER_WINDOWS = "userWindows"

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
    private var _whiteboardFlow = MutableStateFlow<Map<String, Boolean>?>(null)
    private var _classroomStateFlow = MutableStateFlow<ClassroomState?>(null)
    private var _userWindowsFlow = MutableStateFlow<UserWindows?>(null)

    private var _readyFlow = MutableStateFlow<Boolean?>(null)
    private var inited = false

    fun resetRoom(fastRoom: FastRoom) {
        logger.i("[SyncedState] resetRoom, inited: $inited")
        if (inited) {
            clean()
        }
        syncedStore = fastRoom.room.syncedStore

        connectMapStorage(DEVICE_STATE_STORAGE, DeviceState::class.java) {
            _devicesFlow.value = it
        }

        connectMapStorage(ONSTAGE_USERS_STORAGE, Boolean::class.java) {
            _onStagesFlow.value = it
        }

        connectMapStorage(WHITEBOARD_STORAGE, Boolean::class.java) {
            _whiteboardFlow.value = it
        }

        connectStorage(CLASSROOM_STORAGE, ClassroomState::class.java, gson.toJson(ClassroomState())) {
            _classroomStateFlow.value = ClassroomState(
                raiseHandUsers = it.raiseHandUsers,
                ban = it.ban,
            )
        }

        connectUserWindowsStorage()

        inited = true
        _readyFlow.value = true
    }

    private fun parseUserWindowsState(value: String): UserWindows {
        val state = gson.fromJson(value, JsonObject::class.java)
        var grid = listOf<String>()
        val users = mutableMapOf<String, WindowInfo>()
        state.entrySet().forEach {
            if (it.key != "grid") {
                if (!it.value.isJsonNull) {
                    val userWindowInfo = gson.fromJson(it.value, WindowInfo::class.java)
                    users[it.key] = userWindowInfo
                }
            } else {
                if (!it.value.isJsonNull) {
                    grid = gson.fromJson(it.value, Array<String>::class.java).toList()
                }
            }
        }

        return UserWindows(grid, users)
    }

    private fun connectUserWindowsStorage() {
        syncedStore.connectStorage(USER_WINDOWS, "{\"grid\": []}", object : Promise<String> {
            override fun then(value: String) {
                logger.i("[SyncedState] $USER_WINDOWS initial state: $value")
                try {
                    _userWindowsFlow.value = parseUserWindowsState(value)
                } catch (e: Exception) {
                    logger.e("[SyncedState] $USER_WINDOWS initial error: $e")
                }
            }

            override fun catchEx(t: SDKError) {
                logger.e("[SyncedState] $USER_WINDOWS catchEx error: $t")
            }
        })

        syncedStore.addOnStateChangedListener(USER_WINDOWS) { value, diff ->
            logger.i("[SyncedState] $USER_WINDOWS updated: value: $value diff: $diff")
            _userWindowsFlow.value = parseUserWindowsState(value)
        }
    }

    private fun <T> connectStorage(
        storage: String,
        type: Class<T>,
        defaultJson: String = "{}",
        block: (T) -> Unit
    ) {
        syncedStore.connectStorage(storage, defaultJson, object : Promise<String> {
            override fun then(value: String) {
                logger.i("[SyncedState] $storage initial state: $value")
                block(gson.fromJson(value, type))
            }

            override fun catchEx(t: SDKError) {
                logger.e("[SyncedState] $storage catchEx error: $t")
            }
        })

        syncedStore.addOnStateChangedListener(storage) { value, diff ->
            logger.i("[SyncedState] $storage updated: value: $value diff: $diff")
            block(gson.fromJson(value, type))
        }
    }

    private fun <T> connectMapStorage(
        storage: String,
        itemType: Class<T>,
        defaultJson: String = "{}",
        block: (Map<String, T>) -> Unit
    ) {
        syncedStore.connectStorage(storage, defaultJson, object : Promise<String> {
            override fun then(state: String) {
                logger.i("[SyncedState] $storage initial state: $state")
                block(getMapState(state, itemType))
            }

            override fun catchEx(t: SDKError) {
                logger.e("[SyncedState] $storage catchEx error: $t")
            }
        })

        syncedStore.addOnStateChangedListener(storage) { value, diff ->
            logger.i("[SyncedState] $storage updated: value: $value diff: $diff")
            block(getMapState(value, itemType))
        }
    }

    private fun <T> getMapState(state: String, itemType: Class<T>): Map<String, T> {
        val onStageUsers = mutableMapOf<String, T>()
        try {
            val jsonObject = gson.fromJson(state, JsonObject::class.java)
            jsonObject.entrySet().forEach {
                if (!it.value.isJsonNull) {
                    onStageUsers[it.key] = gson.fromJson(it.value, itemType)
                }
            }
        } catch (e: Exception) {
            logger.e(e, "[SyncedState] onStage users parse error!")
        }
        return onStageUsers
    }

    override fun observeSyncedReady(): Flow<Boolean> {
        return _readyFlow.asStateFlow().filterNotNull()
    }

    override fun observeDeviceState(): Flow<Map<String, DeviceState>> {
        return _devicesFlow.asStateFlow().filterNotNull()
    }

    override fun observeOnStage(): Flow<Map<String, Boolean>> {
        return _onStagesFlow.asStateFlow().filterNotNull()
    }

    override fun observeWhiteboard(): Flow<Map<String, Boolean>> {
        return _whiteboardFlow.asStateFlow().filterNotNull()
    }

    override fun observeClassroomState(): Flow<ClassroomState> {
        return _classroomStateFlow.asStateFlow().filterNotNull()
    }

    override fun observeUserWindows(): Flow<UserWindows> {
        return _userWindowsFlow.asStateFlow().filterNotNull()
    }

    private fun clean() {
        if (!inited) return
        _devicesFlow.value = null
        _onStagesFlow.value = null
        _classroomStateFlow.value = null
        _whiteboardFlow.value = null
        _userWindowsFlow.value = null
        syncedStore.disconnectStorage(DEVICE_STATE_STORAGE)
        syncedStore.disconnectStorage(ONSTAGE_USERS_STORAGE)
        syncedStore.disconnectStorage(CLASSROOM_STORAGE)
        syncedStore.disconnectStorage(WHITEBOARD_STORAGE)
        syncedStore.disconnectStorage(USER_WINDOWS)
        inited = false
        _readyFlow.value = false
    }

    override fun updateDeviceState(userId: String, camera: Boolean, mic: Boolean) {
        val devicesState = mapOf(userId to DeviceState(camera, mic))
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gson.toJson(devicesState))
    }

    override fun deleteDeviceState(userId: String) {
        val devicesState = mapOf(userId to null)
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gsonWithNull.toJson(devicesState))
    }

    override fun muteDevicesMic(userIds: List<String>) {
        val deviceState = _devicesFlow.value?.filter { userIds.contains(it.key) } ?: return
        val newState = deviceState.mapValues { DeviceState(it.value.camera, false) }
        syncedStore.setStorageState(DEVICE_STATE_STORAGE, gsonWithNull.toJson(newState))
    }

    override fun updateOnStage(userId: String, onStage: Boolean) {
        if (!onStage) {
            removeMaximizeWindow(userId)
        }

        val jsonObj = mapOf(userId to onStage)
        syncedStore.setStorageState(ONSTAGE_USERS_STORAGE, gson.toJson(jsonObj))
    }

    override fun stageOffAll() {
        removeAllWindow()

        val newState = _onStagesFlow.value?.mapValues { false } ?: return
        syncedStore.setStorageState(ONSTAGE_USERS_STORAGE, gson.toJson(newState))
    }

    override fun updateWhiteboard(userId: String, allowDraw: Boolean) {
        val jsonObj = mapOf(userId to allowDraw)
        syncedStore.setStorageState(WHITEBOARD_STORAGE, gson.toJson(jsonObj))
    }

    override fun updateRaiseHand(userId: String, raiseHand: Boolean) {
        val raiseHandUsers = _classroomStateFlow.value?.raiseHandUsers ?: return
        val users = raiseHandUsers.toMutableSet()
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

    override fun maximizeWindows(userId: String) {
        val userWindows = _userWindowsFlow.value ?: return
        val users = (userWindows.users.keys + userWindows.grid).toMutableList()
        if (!users.contains(userId)) {
            users.add(0, userId)
        }
        val jsonObj = mapOf("grid" to users)
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }

    override fun removeMaximizeWindow(userId: String) {
        val userWindows = _userWindowsFlow.value ?: return
        val users = userWindows.grid.filter { it != userId }
        val jsonObj = mapOf(
            "grid" to users,
            userId to null
        )
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }

    override fun normalizeWindows() {
        val jsonObj = mapOf("grid" to emptyList<String>())
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }

    override fun updateNormalWindow(userId: String, window: WindowInfo) {
        val jsonObj = mapOf(userId to window)
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }

    override fun removeNormalWindow(userId: String) {
        val jsonObj = mapOf(userId to null)
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }

    override fun removeAllWindow() {
        val userWindows = _userWindowsFlow.value ?: return
        val users = userWindows.users.map { it.key to null }.toMap()
        val jsonObj = mapOf("grid" to emptyList<String>()) + users
        syncedStore.setStorageState(USER_WINDOWS, gsonWithNull.toJson(jsonObj))
    }
}