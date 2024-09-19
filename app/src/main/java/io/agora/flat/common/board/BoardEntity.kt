package io.agora.flat.common.board

sealed class BoardPhase {
    object Init : BoardPhase()
    object Connecting : BoardPhase()
    object Connected : BoardPhase()
    object Disconnected : BoardPhase()
    data class Error(val message: String) : BoardPhase()
}

sealed class BoardError {
    object Kicked : BoardError()
    data class Unknown(val message: String) : BoardError()
}