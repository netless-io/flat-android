package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    private val eventBus: EventBus,
) : ViewModel() {
    private val roomUUID = MutableStateFlow("")
    private val loadingCounter = ObservableLoadingCounter()
    private val uiMessageManager = UiMessageManager()

    val state: StateFlow<CreateRoomUiState> = combine(
        roomUUID,
        loadingCounter.observable,
        uiMessageManager.message
    ) { uuid, loading, message ->
        CreateRoomUiState(
            uuid,
            loading,
            userRepository.getUserInfo()!!.name,
            userRepository.getUserInfo()!!.avatar,
            appKVCenter.getDeviceStatePreference(),
            message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreateRoomUiState.by(
            username = userRepository.getUserInfo()!!.name,
            deviceState = appKVCenter.getDeviceStatePreference()
        ),
    )

    fun createRoom(title: String, type: RoomType) {
        viewModelScope.launch {
            loadingCounter.addLoader()
            when (val result = runAtLeast { roomRepository.createOrdinary(title, type) }) {
                is Success -> {
                    roomUUID.value = result.get().roomUUID
                    eventBus.produceEvent(RoomsUpdated)
                }
                is Failure -> {
                    uiMessageManager.emitMessage(UiMessage("create ordinary room failure", result.exception))
                }
            }
            loadingCounter.removeLoader()
        }
    }

    fun updateDeviceState(cameraOn: Boolean, micOn: Boolean) {
        viewModelScope.launch {
            appKVCenter.setDeviceStatePreference(DeviceState(camera = cameraOn, mic = micOn))
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class CreateRoomUiState(
    val roomUUID: String = "",
    val loading: Boolean = false,
    val username: String = "",
    val avatar: String? = null,
    val deviceState: DeviceState,
    val message: UiMessage? = null
) {
    companion object {
        fun by(username: String, deviceState: DeviceState): CreateRoomUiState {
            return CreateRoomUiState(username = username, deviceState = deviceState)
        }
    }
}