package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Success
import io.agora.flat.data.model.RecordInfo
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val savedStateHandle: SavedStateHandle,
    private val database: AppDatabase,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter,
) : ViewModel() {
    private lateinit var _state: MutableStateFlow<ReplayState>
    val state: StateFlow<ReplayState>
        get() = _state

    val roomUUID: String

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

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }
}

data class ReplayState(
    val roomUUID: String,
    val roomInfo: RoomInfo? = null,
    val recordInfo: RecordInfo? = null,
)