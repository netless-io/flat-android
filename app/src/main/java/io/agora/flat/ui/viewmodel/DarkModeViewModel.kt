package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.DarkModeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DarkModeViewModel @Inject constructor() : ViewModel() {
    private var _state = MutableStateFlow(obtainInitState())
    val state = _state.asStateFlow()

    fun selectMode(mode: DarkModeManager.Mode) {
        _state.value = _state.value.copy(current = mode)
    }

    fun save() {
        DarkModeManager.update(state.value.current)
    }

    private fun obtainInitState(): DarkModeUIState {
        val current = DarkModeManager.current()
        val modes = listOf(DarkModeManager.Mode.Auto, DarkModeManager.Mode.Dark, DarkModeManager.Mode.Light)
        return DarkModeUIState(current, modes)
    }
}

data class DarkModeUIState(
    val current: DarkModeManager.Mode,
    val modes: List<DarkModeManager.Mode>,
)