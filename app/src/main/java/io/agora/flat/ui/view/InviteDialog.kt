package io.agora.flat.ui.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    override fun onStart() {
        markNotFocusable()
        super.onStart()
        unmarkNotFocusable()
        hideBars()
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onHide()
    }

    interface Listener {
        fun onCopy()
        fun onHide()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private fun markNotFocusable() {
        dialog?.window?.setFlags(FLAG_NOT_FOCUSABLE, FLAG_NOT_FOCUSABLE)
    }

    private fun unmarkNotFocusable() {
        dialog?.window?.clearFlags(FLAG_NOT_FOCUSABLE)
    }

    private fun hideBars() {
        val window = dialog?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}