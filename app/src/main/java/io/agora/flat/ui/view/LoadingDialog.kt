package io.agora.flat.ui.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import io.agora.flat.R
import io.agora.flat.databinding.DialogLoadingBinding

class LoadingDialog : ClassDialogFragment(R.layout.dialog_loading) {
    private lateinit var binding: DialogLoadingBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogLoadingBinding.bind(view)
    }
}