package io.agora.flat.ui.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import io.agora.flat.R
import io.agora.flat.databinding.DialogReplayExitBinding

class ReplayExitDialog : DialogFragment(R.layout.dialog_replay_exit) {
    private lateinit var binding: DialogReplayExitBinding
    private var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogReplayExitBinding.bind(view)

        binding.leftButton.setOnClickListener {
            listener?.onLeftButtonClick()
            dismiss()
        }

        binding.rightButton.setOnClickListener {
            listener?.onRightButtonClick()
            dismiss()
        }

        binding.close.setOnClickListener {
            listener?.onClose()
            dismiss()
        }
    }

    interface Listener {
        fun onClose()
        fun onLeftButtonClick()
        fun onRightButtonClick()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}