package io.agora.flat.ui.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import io.agora.flat.R
import io.agora.flat.databinding.DialogInviteBinding

class InviteDialog : DialogFragment(R.layout.dialog_invite) {
    private lateinit var binding: DialogInviteBinding
    private var listener: Listener? = null

    companion object {
        const val INVITE_TITLE = "invite_title"
        const val ROOM_TITLE = "room_title"
        const val ROOM_NUMBER = "room_number"
        const val ROOM_TIME = "room_time"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogInviteBinding.bind(view)

        binding.copy.setOnClickListener {
            listener?.onCopy()
            dismiss()
        }
        binding.inviteTitle.text = arguments?.getString(INVITE_TITLE, "")
        binding.roomTitle.text = arguments?.getString(ROOM_TITLE, "")
        binding.roomNumber.text = arguments?.getString(ROOM_NUMBER, "")
        binding.roomTime.text = arguments?.getString(ROOM_TIME, "")
    }

    override fun onDismiss(dialog: DialogInterface) {
        // super.onDismiss(dialog)
        listener?.onHide()
    }

    interface Listener {
        fun onCopy()
        fun onHide()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}