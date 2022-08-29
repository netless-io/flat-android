package io.agora.flat.common.board

sealed class BoardRoomPhase {
    object Init : BoardRoomPhase()
    object Connecting : BoardRoomPhase()
    object Connected : BoardRoomPhase()
    object Disconnected : BoardRoomPhase()
    data class Error(val message: String) : BoardRoomPhase()
}