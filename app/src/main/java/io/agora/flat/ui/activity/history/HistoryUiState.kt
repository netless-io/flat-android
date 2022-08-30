package io.agora.flat.ui.activity.history

import io.agora.flat.data.model.RoomInfo

data class HistoryUiState(
    val histories: List<RoomInfo> = listOf(),
    val refreshing: Boolean = true,
    val noMore: Boolean = true,
) {
    companion object {
        val Empty = HistoryUiState()
    }
}