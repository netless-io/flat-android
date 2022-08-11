package io.agora.flat.ui.activity.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.common.board.BoardRoomPhase
import io.agora.flat.ui.util.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val boardRoom: BoardRoom,
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
                        _state.value = _state.value.copy(error = UiError(phase.message))
                    }
                    else -> {; }
                }
            }
        }
    }
}

data class ExtensionState(
    val loading: Boolean = true,
    val error: UiError? = null,
)