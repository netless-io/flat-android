package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val database: AppDatabase,
) : ViewModel() {
    private val roomUUID = MutableStateFlow<String>("")
    private val loading = MutableStateFlow(false)
    private val loadingCount = AtomicInteger(0)

    private val _state = MutableStateFlow(ViewState())

    val state: StateFlow<ViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(roomUUID, loading) { roomUUID, loading ->
                ViewState(
                    roomUUID = roomUUID,
                    loading = loading
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }
    }

    private fun incLoadingCount() {
        loadingCount.incrementAndGet()
        loading.value = true
    }

    private fun decLoadingCount() {
        if (loadingCount.decrementAndGet() == 0) {
            loading.value = false
        }
    }

    fun createRoom(title: String, type: RoomType) {
        viewModelScope.launch {
            incLoadingCount()
            // TODO
            delay(5000)
            when (val result = roomRepository.createOrdinary(title, type)) {
                is Success -> {
                    roomUUID.value = result.get().roomUUID
                }
                else -> {
                }
            }
            decLoadingCount()
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
) {
    companion object {
        const val STATE_IDLE = 0;
        const val STATE_LOADING = 1;
        const val STATE_CREATE_SUCCESS = 1;
        const val STATE_CREATE_FAIL = 1;
    }
}

internal sealed class CreateRoomAction {
    object Close : CreateRoomAction()
    data class JoinRoom(val roomUUID: String, val openVideo: Boolean) : CreateRoomAction()
    data class CreateRoom(val title: String, val roomType: RoomType) : CreateRoomAction()
}