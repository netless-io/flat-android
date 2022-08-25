package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val database: AppDatabase,
    private val eventBus: EventBus,
) : ViewModel() {
    private val username = userRepository.getUserInfo()!!.name

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
            username,
            message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreateRoomUiState.byUsername(username),
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

    fun enableVideo(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            database.roomConfigDao().insertOrUpdate(RoomConfig(roomUUID.value, enable, enable))
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
    val message: UiMessage? = null
) {
    companion object {
        fun byUsername(username: String): CreateRoomUiState {
            return CreateRoomUiState(username = username)
        }
    }
}