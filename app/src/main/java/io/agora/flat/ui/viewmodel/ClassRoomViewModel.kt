package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.agora.flat.Constants
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomRepository
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _joining = MutableStateFlow<Boolean>(true)
    val joining: SharedFlow<Boolean> = _joining

    private var _roomUsersMap = MutableStateFlow<Map<String, RtcUser>>(emptyMap())
    val roomUsersMap = _roomUsersMap.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result =
                roomRepository.joinRoom(intentValue(Constants.IntentKey.ROOM_UUID))) {
                is Success -> {
                    _roomPlayInfo.value = result.data
                }
                is ErrorResult -> {

                }
            }
            _joining.value = false
        }

        viewModelScope.launch {
            fetchRoomInfo(intentValue(Constants.IntentKey.ROOM_UUID))
        }
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }

    fun requestRoomUsers(roomUUID: String, usersUUID: List<String>) {
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, usersUUID)) {
                is Success -> {
                    _roomUsersMap.value = result.data;
                }
                is ErrorResult -> {

                }
            }
        }
    }

    suspend fun fetchRoomInfo(roomUUID: String): RoomInfo? {
        return null;
    }
}