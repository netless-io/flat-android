package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.manager.JoinRoomRecordManager
import io.agora.flat.data.model.JoinRoomRecord
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val appKVCenter: AppKVCenter,
    private val joinRoomRecordManager: JoinRoomRecordManager,
) : ViewModel() {
    private val uiMessageManager = UiMessageManager()

    private var _state = MutableStateFlow(
        JoinRoomUiState.by(
            deviceState = appKVCenter.getDeviceStatePreference(),
            avatar = userRepository.getUserInfo()?.avatar,
            records = joinRoomRecordManager.getRecordList().items
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            uiMessageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }
    }

    fun joinRoom(uuid: String, cameraOn: Boolean, micOn: Boolean) {
        val trimID = uuid.replace("\\s".toRegex(), "")
        viewModelScope.launch {
            when (val result = roomRepository.joinRoom(trimID)) {
                is Success -> {
                    appKVCenter.setDeviceStatePreference(DeviceState(camera = cameraOn, mic = micOn))
                    _state.value = _state.value.copy(roomPlayInfo = result.data)
                }

                is Failure -> uiMessageManager.emitMessage(UiMessage("join room error", result.exception))
            }
        }
    }

    fun clearJoinRoomRecord() {
        viewModelScope.launch {
            joinRoomRecordManager.clearRecords()
            _state.value = _state.value.copy(records = listOf())
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class JoinRoomUiState(
    val roomPlayInfo: RoomPlayInfo? = null,
    val avatar: String? = null,
    val deviceState: DeviceState,
    val message: UiMessage? = null,
    val records: List<JoinRoomRecord>,
) {
    companion object {
        fun by(
            deviceState: DeviceState,
            avatar: String? = null,
            records: List<JoinRoomRecord> = listOf()
        ): JoinRoomUiState {
            return JoinRoomUiState(deviceState = deviceState, avatar = avatar, records = records)
        }
    }
}