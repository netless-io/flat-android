package io.agora.flat.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.Success
import io.agora.flat.data.model.RecordInfo
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.data.repository.MessageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.RtmEngineProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val rtmApi: RtmEngineProvider,
) : ViewModel() {
    private var _state: MutableStateFlow<ReplayState>
    val state: StateFlow<ReplayState>
        get() = _state

    val roomUUID: String
    var query: MessageQuery? = null

    init {
        roomUUID = intentValue(Constants.IntentKey.ROOM_UUID)
        _state = MutableStateFlow(
            ReplayState(roomUUID = roomUUID)
        )

        viewModelScope.launch {
            val roomResp = roomRepository.getOrdinaryRoomInfo(roomUUID)
            if (roomResp is Success) {
                if (roomResp.data.roomInfo.roomStatus != RoomStatus.Stopped) {
                    return@launch
                }
                val recordResp = cloudRecordRepository.getRecordInfo(roomUUID)
                if (recordResp is Success) {
                    _state.value = _state.value.copy(roomInfo = roomResp.data.roomInfo, recordInfo = recordResp.data)

                    query = MessageQuery(
                        _state.value.roomUUID,
                        _state.value.roomInfo!!.beginTime,
                        _state.value.roomInfo!!.endTime,
                        rtmApi,
                        UserQuery(roomUUID, userRepository, roomRepository)
                    )

                    try {
                        repeat(3) {
                            delay(2000)
                            val result = messageRepository.getMessageCount(
                                _state.value.roomUUID,
                                _state.value.roomInfo!!.beginTime,
                                _state.value.roomInfo!!.endTime,
                            )
                            if (result is Success) {
                                Log.e("Aderan", "message count is ${result.data}")
                            }
                        }
                    } catch (e: Exception) {

                    }
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
            val msgs = query?.query(time) ?: emptyList()
            _state.value = _state.value.copy(messages = msgs.toList())
        }
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }
}

data class ReplayState(
    val roomUUID: String,
    val roomInfo: RoomInfo? = null,
    val recordInfo: RecordInfo? = null,
    val messages: List<RTMMessage> = listOf(),

    val isPlayer: Boolean = false,
) {
    val duration: Long
        get() = (roomInfo?.run { endTime - beginTime }) ?: 0L
}