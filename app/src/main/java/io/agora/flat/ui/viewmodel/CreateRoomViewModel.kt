package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.impl.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val database: AppDatabase,
    private val eventBus: EventBus,
) : ViewModel() {
    private val roomUUID = MutableStateFlow("")
    private val loadingCounter = ObservableLoadingCounter()

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(roomUUID, loadingCounter.observable) { roomUUID, loading ->
                _state.value.copy(roomUUID = roomUUID, loading = loading)
            }.collect {
                _state.value = it
            }
        }
        _state.value = _state.value.copy(username = "${userRepository.getUserInfo()?.name}")
    }

    fun createRoom(title: String, type: RoomType) {
        viewModelScope.launch {
            loadingCounter.addLoader()
            when (val result = runAtLeast { roomRepository.createOrdinary(title, type) }) {
                is Success -> {
                    roomUUID.value = result.get().roomUUID
                    eventBus.produceEvent(RoomsUpdated)
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
}

data class ViewState(
    val roomUUID: String = "",
    val loading: Boolean = false,
    val state: Int = 0,
    val username: String = "",
) {
    companion object {
        const val STATE_IDLE = 0
        const val STATE_LOADING = 1
        const val STATE_CREATE_SUCCESS = 1
        const val STATE_CREATE_FAIL = 1
    }
}