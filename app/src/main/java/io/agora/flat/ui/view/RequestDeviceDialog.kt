package io.agora.flat.ui.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.databinding.DialogRequestDeviceBinding

class RequestDeviceDialog : ClassDialogFragment(R.layout.dialog_request_device) {
    private lateinit var binding: DialogRequestDeviceBinding
    private var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnCancelListener {
            listener?.onRefuse()
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogRequestDeviceBinding.bind(view)
        binding.message.text = arguments?.getString(Constants.IntentKey.MESSAGE) ?: ""
        binding.leftButton.setOnClickListener {
            listener?.onRefuse()
            dismiss()
        }

        binding.rightButton.setOnClickListener {
            listener?.onAgree()
            dismiss()
        }

        binding.close.setOnClickListener {
            listener?.onRefuse()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {

    }

    interface Listener {
        fun onRefuse()

        fun onAgree()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}