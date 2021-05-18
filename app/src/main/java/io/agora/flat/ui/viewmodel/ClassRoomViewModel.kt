package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.FlatException
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.AppModule
import io.agora.flat.di.interfaces.RtmEngineProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle,
    private val database: AppDatabase,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter,
    private val rtmApi: RtmEngineProvider
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _roomInfo = MutableStateFlow<RoomInfo?>(null)
    val roomInfo = _roomInfo.asStateFlow()

    private var _usersCacheMap: MutableMap<String, RtcUser> = mutableMapOf()
    private var _currentUsersMap = MutableStateFlow<Map<String, RtcUser>>(emptyMap())
    val currentUsersMap = _currentUsersMap.asStateFlow()

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _roomEvent = MutableStateFlow<ClassRoomEvent?>(null)
    val roomEvent = _roomEvent
    val uuid = AtomicInteger(0)

    var roomUUID: String

    private var _roomConfig = MutableStateFlow(RoomConfig(""))
    val roomConfig = _roomConfig.asStateFlow()

    init {
        roomUUID = intentValue(Constants.IntentKey.ROOM_UUID)
        viewModelScope.launch {
            when (val result =
                roomRepository.joinRoom(roomUUID)) {
                is Success -> {
                    _roomPlayInfo.value = result.data
                }
                is ErrorResult -> {

                }
            }
        }

        viewModelScope.launch {
            when (val result =
                roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> {
                    _roomInfo.value = result.data.roomInfo;
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _roomConfig.value = database.roomConfigDao().getConfigById(roomUUID) ?: RoomConfig(roomUUID)
        }
    }

    fun isVideoEnable(): Boolean {
        return _roomConfig.value.enableVideo
    }

    fun isAudioEnable(): Boolean {
        return _roomConfig.value.enableVideo
    }

    fun enableVideo(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = _roomConfig.value.copy(enableVideo = enable)

            database.roomConfigDao().insertOrUpdate(config)
            _roomConfig.value = config

            sendDeviceState(config)
        }
    }

    private suspend fun sendDeviceState(config: RoomConfig) {
        val event = RTMEvent.DeviceState(
            DeviceStateValue(
                userUUID = appKVCenter.getUserInfo()!!.uuid,
                camera = config.enableVideo,
                mic = config.enableAudio
            )
        )
        rtmApi.sendChannelCommand(event)
        updateDeviceState(event.value)
    }

    fun enableAudio(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = _roomConfig.value.copy(enableAudio = enable)

            database.roomConfigDao().insertOrUpdate(config)
            _roomConfig.value = config

            sendDeviceState(config)
        }
    }

    suspend fun initRoomUsers(usersUUIDs: List<String>): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, usersUUIDs)) {
                is Success -> {
                    result.data.forEach { (uuid, user) -> user.userUUID = uuid }
                    addToCache(result.data)
                    addToCurrent(result.data)
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resumeWithException(FlatException(result.error.code, result.error.message))
                }
            }
        }
    }

    fun onEvent(event: ClassRoomEvent) {
        viewModelScope.launch {
            _roomEvent.value = event
        }
    }

    fun onOperationAreaShown(areaId: Int) {
        onEvent(ClassRoomEvent.OperationAreaShown(areaId))
    }

    private fun addToCurrent(userMap: Map<String, RtcUser>) {
        val map = _currentUsersMap.value.toMutableMap()
        userMap.forEach { (uuid, user) -> map[uuid] = user }
        _currentUsersMap.value = map
    }

    private fun addToCache(userMap: Map<String, RtcUser>) {
        userMap.forEach { (uuid, user) -> _usersCacheMap[uuid] = user }
    }

    fun removeRtmMember(userUUID: String) {
        val map = _currentUsersMap.value.toMutableMap()
        map.remove(userUUID)
        _currentUsersMap.value = map
    }

    fun addRtmMember(userUUID: String) {
        if (_usersCacheMap.containsKey(userUUID)) {
            addToCurrent(mapOf(userUUID to _usersCacheMap[userUUID]!!))
            return
        }
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, listOf(userUUID))) {
                is Success -> {
                    result.data.forEach { (uuid, user) -> user.userUUID = uuid }
                    addToCache(result.data)
                    addToCurrent(result.data)
                }
                is ErrorResult -> {
                }
            }
        }
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }

    fun setVideoShown(shown: Boolean) {
        _videoAreaShown.value = shown
    }

    // RTCCommand Handle
    fun updateDeviceState(value: DeviceStateValue) {
        val map = _currentUsersMap.value.toMutableMap()
        val user = map[value.userUUID]
        if (user != null) {
            val copy = user.copy(audioOpen = value.mic, videoOpen = value.camera)
            map[value.userUUID] = copy
        }
        _currentUsersMap.value = map
    }

    fun updateUserState(userUUID: String, state: RTMUserState) {
        val map = _currentUsersMap.value.toMutableMap()
        val user = map[userUUID]
        if (user != null) {
            val copy = user.copy(
                audioOpen = state.mic,
                videoOpen = state.camera,
                name = state.name,
                isSpeak = state.isSpeak
            )
            map[userUUID] = copy
            _currentUsersMap.value = map
        }
    }

    fun updateChannelState(status: ChannelStatusValue) {
        val map = _currentUsersMap.value.toMutableMap()
        status.uStates.forEach { (userId, s) ->
            run {
                val user = map[userId]
                if (user != null) {
                    user.videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true)
                    user.audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true)
                    user.isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true)
                    user.isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true)
                }
            }
        }
        _currentUsersMap.value = map
    }

    fun requestChannelStatus() {
        viewModelScope.launch {
            val user =
                _currentUsersMap.value.values.firstOrNull { rtcUser -> rtcUser.userUUID != appKVCenter.getUserInfo()!!.uuid }
            if (user != null) {
                val roomUUID = roomUUID;
                val userInfo = appKVCenter.getUserInfo()!!
                val state = RTMUserState(userInfo.name, camera = false, mic = false, isSpeak = false)
                val value = RequestChannelStatusValue(roomUUID, listOf(user.userUUID), state)
                val event = RTMEvent.RequestChannelStatus(value)

                rtmApi.sendChannelCommand(event)
            }
        }
    }
}

sealed class ClassRoomEvent {
    companion object {
        const val AREA_ID_APPLIANCE = 1
        const val AREA_ID_PAINT = 2
        const val AREA_ID_SETTING = 3
        const val AREA_ID_MESSAGE = 4
        const val AREA_ID_CLOUD_STORAGE = 4
    }

    object EnterRoom : ClassRoomEvent()
    object RtmChannelJoined : ClassRoomEvent()
    data class ChangeVideoDisplay(val id: Int) : ClassRoomEvent()
    data class OperationAreaShown(val areaId: Int) : ClassRoomEvent()
}