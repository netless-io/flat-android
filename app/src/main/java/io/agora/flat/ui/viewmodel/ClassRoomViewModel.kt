package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _roomInfo = MutableStateFlow<RoomInfo?>(null)
    val roomInfo = _roomInfo.asStateFlow()

    private var _usersCacheMap: MutableMap<String, RtcUser> = mutableMapOf()
    private var _currentUsersMap = MutableStateFlow<Map<String, RtcUser>>(emptyMap())
    val currentUsersMap = _currentUsersMap.asStateFlow()

    private var _videoAreaShown = MutableStateFlow<Boolean>(false)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _roomEvent = MutableStateFlow<ClassRoomEvent?>(null)
    val roomEvent = _roomEvent
    val uuid = AtomicInteger(0)

    private val roomUUID: String
    private var videoShown: Boolean = false

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
                is ErrorResult -> {

                }
            }
        }
        viewModelScope.launch {
            _roomEvent.value = ClassRoomEvent.EnterRoom
        }
    }

    fun requestRoomUsers(usersUUID: List<String>) {
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, usersUUID)) {
                is Success -> {
                    addToCache(result.data)
                    addToCurrent(result.data)
                }
                is ErrorResult -> {

                }
            }
        }
    }

    fun onEvent(event: ClassRoomEvent) {
        viewModelScope.launch {
            _roomEvent.value = event
        }
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
}

sealed class ClassRoomEvent {
    object EnterRoom : ClassRoomEvent()
    object RtmChannelJoined : ClassRoomEvent()
    data class ChangeVideoDisplay(val id: Int) : ClassRoomEvent()
}