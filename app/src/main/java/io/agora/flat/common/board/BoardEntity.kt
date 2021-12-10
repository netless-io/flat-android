package io.agora.flat.common.board

data class BoardSceneState(val scenes: List<SceneItem>, val index: Int)

data class SceneItem(val scenePath: String, val preview: String?)

data class UndoRedoState(val undoCount: Long, val redoCount: Long)

sealed class BoardRoomPhase {
    object Init : BoardRoomPhase()
    object Connecting : BoardRoomPhase()
    object Connected : BoardRoomPhase()
    object Disconnected : BoardRoomPhase()
    data class Error(val message: String) : BoardRoomPhase()
}