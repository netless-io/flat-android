package io.agora.flat.ui.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import io.agora.flat.R
import io.agora.flat.databinding.LayoutMessageListBinding
import io.agora.flat.ui.activity.play.MessageAdapter
import io.agora.flat.ui.viewmodel.RTMMessage

class MessageListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: LayoutMessageListBinding =
        LayoutMessageListBinding.inflate(LayoutInflater.from(context), this, true)
    private var messageAdapter: MessageAdapter = MessageAdapter()

    private var listener: Listener? = null

    init {
        binding.messageList.adapter = messageAdapter
        binding.messageList.layoutManager = LinearLayoutManager(context)

        binding.messageEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
            }
            true
        }

        binding.send.setOnClickListener {
            val msg = binding.messageEdit.text.toString()
            if (msg.isNotEmpty()) {
                binding.messageEdit.setText("")
                binding.messageEdit.clearFocus()
                listener?.onSendMessage(msg)
            }
        }
    }

    fun setBan(ban: Boolean) {
        binding.send.isEnabled = !ban
        binding.messageEdit.isEnabled = !ban
        binding.messageEdit.hint =
            if (ban) context.getString(R.string.class_room_message_muted) else context.getString(R.string.say_something)
    }

    fun setEditable(enable: Boolean) {
        binding.messageEdit.isEnabled = enable
    }

    private fun hideKeyboard() {
        if (context is Activity) {
            (context as Activity).currentFocus?.let { view ->
                val imm = (context as Activity).getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    fun setMessages(messages: List<RTMMessage>) {
        messageAdapter.setDataList(messages)
        binding.messageList.smoothScrollToPosition(messages.size);
        binding.listEmpty.isVisible = messages.isEmpty()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun interface Listener {
        fun onSendMessage(msg: String)
    }
}