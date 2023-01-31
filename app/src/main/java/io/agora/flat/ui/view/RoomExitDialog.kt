package io.agora.flat.ui.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.databinding.DialogRoomExitBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class RoomExitDialog : ClassDialogFragment(R.layout.dialog_room_exit) {
    private lateinit var binding: DialogRoomExitBinding
    private var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogRoomExitBinding.bind(view)

        binding.message.text = arguments?.getString(Constants.IntentKey.MESSAGE, "")

        binding.centerButton.setOnClickListener {
            listener?.onFinish()
            dismissAllowingStateLoss()
        }

        lifecycleScope.launchWhenStarted {
            var second = 5
            tickerFlow(1000).collect {
                binding.centerButton.text = getString(R.string.exit_room_i_known_format, second)
                if (--second < 0) {
                    listener?.onFinish()
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    private fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    fun interface Listener {
        fun onFinish()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}