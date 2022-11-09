package io.agora.flat.ui.activity.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.board.AgoraBoardRoom
import io.agora.flat.common.board.BoardRoomPhase
import io.agora.flat.ui.manager.RoomErrorManager
import io.agora.flat.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val errorManager: RoomErrorManager,
    private val boardRoom: AgoraBoardRoom,
) : ViewModel() {
    private val _state = MutableStateFlow(ExtensionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            boardRoom.observeRoomPhase().collect { phase ->
                when (phase) {
                    BoardRoomPhase.Connecting -> {
                        _state.value = _state.value.copy(loading = true)
                    }
                    BoardRoomPhase.Connected -> {
                        _state.value = _state.value.copy(loading = false)
                    }
                    is BoardRoomPhase.Error -> {
                        _state.value = _state.value.copy(error = UiMessage(phase.message))
                    }
                    else -> {; }
                }
            }
        }

        viewModelScope.launch {
            errorManager.observeError().collect {
                _state.value = _state.value.copy(error = it)
            }
        }
    }
}

data class ExtensionState(
    val loading: Boolean = true,
    val error: UiMessage? = null,
)