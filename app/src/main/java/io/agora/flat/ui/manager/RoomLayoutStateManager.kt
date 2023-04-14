package io.agora.flat.ui.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 房间布局状态管理
 */
object RoomLayoutStateManager {
    private var state = MutableStateFlow(RoomLayoutState())

    fun observeState() = state.asStateFlow()

    fun setToolboxMarginBottom(bottom: Int) {
        state.value = state.value.copy(toolboxMarginBottom = bottom)
    }
}

data class RoomLayoutState(
    val toolboxMarginBottom: Int = 0,
)