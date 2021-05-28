package io.agora.flat.ui.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
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
        // TODO Dialog导致的Window切换处理
        // dialog.window?.setFlags(FLAG_NOT_FOCUSABLE, FLAG_NOT_FOCUSABLE)
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

        // hideSystemUI()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // super.onDismiss(dialog)
        listener?.onHide()
    }

    private fun hideSystemUI() {
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    interface Listener {
        fun onCopy()
        fun onHide()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}