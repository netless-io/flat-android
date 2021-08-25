package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.message.Message
import io.agora.flat.data.Success
import io.agora.flat.data.model.RecordInfo
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val messageQuery: MessageQuery,
) : ViewModel() {
    private var _state: MutableStateFlow<ReplayState>
    val state: StateFlow<ReplayState>
        get() = _state

    val roomUUID: String = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!

    init {
        _state = MutableStateFlow(ReplayState(roomUUID = roomUUID))

        viewModelScope.launch {
            val resp = roomRepository.getOrdinaryRoomInfo(roomUUID)
            if (resp is Success) {
                val roomInfo = resp.data.roomInfo
                if (roomInfo.roomStatus != RoomStatus.Stopped) {
                    return@launch
                }
                val recordResp = cloudRecordRepository.getRecordInfo(roomUUID)
                if (recordResp is Success) {
                    messageQuery.update(roomUUID, roomInfo.beginTime, roomInfo.endTime)
                    _state.value = _state.value.copy(roomInfo = roomInfo, recordInfo = recordResp.data)
                }
            }
        }

        viewModelScope.launch {
            val result = cloudRecordRepository.getRecordInfo(roomUUID)
            if (result is Success) {
                _state.value = _state.value.copy(recordInfo = result.data)
            }
        }
    }

    fun updateTime(time: Long) {
        viewModelScope.launch {
            val msgs = messageQuery.query(time)
            _state.value = _state.value.copy(messages = msgs.toList())
        }
    }
}

data class ReplayState(
    val roomUUID: String,
    val roomInfo: RoomInfo? = null,
    val recordInfo: RecordInfo? = null,
    val messages: List<Message> = listOf(),

    val isPlayer: Boolean = false,
) {
    val duration: Long
        get() = (roomInfo?.run { endTime - beginTime }) ?: 0L
}