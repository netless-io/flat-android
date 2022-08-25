package io.agora.flat.ui.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.common.rtm.Message
import io.agora.flat.databinding.LayoutMessageListBinding
import io.agora.flat.ui.activity.play.MessageAdapter


class MessageListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: LayoutMessageListBinding = LayoutMessageListBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    private var messageAdapter: MessageAdapter = MessageAdapter(context)
    private var listener: Listener? = null
    private var firstVisible = -1

    init {
        binding.messageList.adapter = messageAdapter
        binding.messageList.layoutManager = LinearLayoutManager(context)
        binding.messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isLoading() || recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    return
                }
                val layoutManager = binding.messageList.layoutManager as LinearLayoutManager
                val first = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible != first) {
                    firstVisible = first
                    if (first == 0) listener?.onLoadMore()
                }
            }
        })

        binding.messageEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                hideKeyboard()
            }
            true
        }

        binding.messageEdit.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }

        binding.send.setOnClickListener {
            val msg = binding.messageEdit.text.toString()
            if (msg.isNotEmpty()) {
                binding.messageEdit.setText("")
                binding.messageEdit.clearFocus()
                listener?.onSendMessage(msg)
                hideKeyboard(binding.messageEdit)
            }
        }

        binding.mute.setOnClickListener {
            val targetMuted = !binding.mute.isSelected
            binding.mute.isSelected = targetMuted
            listener?.onMute(targetMuted)
        }

        // Consume events to prevent edittext focus loss
        binding.sendLayout.setOnClickListener {}
    }

    fun setBan(ban: Boolean, isOwner: Boolean) {
        binding.mute.isSelected = ban

        val canSend = isOwner || !ban
        binding.send.isEnabled = canSend
        binding.messageEdit.isEnabled = canSend
        binding.messageEdit.hint = if (canSend) {
            context.getString(R.string.say_something)
        } else {
            context.getString(R.string.class_room_message_muted)
        }
    }

    fun showBanBtn(shown: Boolean) {
        binding.mute.isVisible = shown
    }

    fun setEditable(enable: Boolean) {
        binding.messageEdit.isEnabled = enable
    }

    private fun hideKeyboard(view: View? = null) {
        val viewTarget = view ?: (context as? Activity)?.currentFocus
        viewTarget?.let { v ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun showLoading(loading: Boolean) {
        binding.loading.isVisible = loading
    }

    private fun isLoading() = binding.loading.isVisible

    fun setMessages(messages: List<Message>) {
        messageAdapter.setMessages(messages)
        binding.listEmpty.isVisible = messages.isEmpty()
        postDelayed({ binding.messageList.scrollToPosition(messageAdapter.itemCount - 1) }, 100)
    }

    fun addMessagesAtHead(messages: List<Message>) {
        messageAdapter.addMessagesAtHead(messages)
    }

    fun addMessagesAtTail(messages: List<Message>) {
        messageAdapter.addMessagesAtTail(messages)
        postDelayed({ binding.messageList.scrollToPosition(messageAdapter.itemCount - 1) }, 100)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onSendMessage(msg: String)

        fun onMute(muted: Boolean)

        fun onLoadMore() {}
    }
}