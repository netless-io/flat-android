package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.rtm.Message
import io.agora.flat.data.Failure
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
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val messageQuery: MessageQuery,
) : ViewModel() {
    private val roomUUID: String = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!
    private var _state = MutableStateFlow(ReplayUiState(roomUUID = roomUUID))
    val state: StateFlow<ReplayUiState>
        get() = _state

    init {
        viewModelScope.launch {
            when (val roomResp = roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> {
                    val roomInfo = roomResp.data.roomInfo
                    if (roomInfo.roomStatus != RoomStatus.Stopped) {
                        return@launch
                    }
                    when (val recordResp = cloudRecordRepository.getRecordInfo(roomUUID)) {
                        is Success -> {
                            messageQuery.update(roomUUID, roomInfo.beginTime, roomInfo.endTime)
                            _state.value = state.value.copy(
                                recordInfo = recordResp.data,
                                roomInfo = roomInfo
                            )
                        }
                        is Failure -> {

                        }
                    }
                }
                is Failure -> {

                }
            }
        }
    }

    fun updateTime(time: Long) {
        // viewModelScope.launch {
        //     val msgs = messageQuery.query(time)
        //     _state.value = _state.value.copy(messages = msgs.toList())
        // }
    }
}

data class ReplayUiState(
    val roomUUID: String,
    val roomInfo: RoomInfo? = null,
    val recordInfo: RecordInfo? = null,
    val users: List<RelayUiUser> = listOf(),
    val messages: List<Message> = listOf(),
    val isPlayer: Boolean = false,
) {
    val duration: Long
        get() = (roomInfo?.run { endTime - beginTime }) ?: 0L
}

data class RelayUiUser(
    val userUUID: String,
    val rtcUID: Int,
    val name: String,
    val avatarURL: String,
    val videoUrl: String,
)