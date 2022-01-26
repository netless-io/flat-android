package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.ViewGroup
import io.agora.board.fast.model.FastWindowBoxState
import io.agora.board.fast.ui.RedoUndoLayout
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.board.fast.ui.ScenesLayout
import io.agora.board.fast.ui.ToolboxLayout
import io.agora.flat.R
import io.agora.flat.util.isPhoneMode

class FlatControllerGroup(root: ViewGroup) : RoomControllerGroup(root) {
    private var redoUndoLayout: RedoUndoLayout? = null
    private var scenesLayout: ScenesLayout? = null
    private var toolboxLayout: ToolboxLayout? = null

    private fun setupView() {
        LayoutInflater.from(context).inflate(R.layout.layout_flat_controller_group, root, true)
        redoUndoLayout = root.findViewById(R.id.redo_undo_layout)
        scenesLayout = root.findViewById(R.id.scenes_layout)
        toolboxLayout = root.findViewById(R.id.toolbox_layout)

        addController(redoUndoLayout)
        addController(scenesLayout)
        addController(toolboxLayout)

        if (context.isPhoneMode()) {
            redoUndoLayout?.hide()
            scenesLayout?.hide()
        }
    }

    override fun updateWindowBoxState(windowBoxState: String) {
        super.updateWindowBoxState(windowBoxState)
        if (context.isPhoneMode()) {
            return
        }
        when (FastWindowBoxState.of(windowBoxState)) {
            FastWindowBoxState.Maximized -> {
                redoUndoLayout?.hide()
                scenesLayout?.hide()
            }
            FastWindowBoxState.Minimized, FastWindowBoxState.Normal -> {
                redoUndoLayout?.show()
                scenesLayout?.show()
            }
        }
    }

    init {
        setupView()
    }
}