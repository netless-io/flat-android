package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.ui.viewmodel.ChatMessage
import io.agora.flat.ui.viewmodel.NoticeMessage
import io.agora.flat.ui.viewmodel.RTMMessage

/**
 * 消息列表适配器
 */
class MessageAdapter constructor(
    private val dataSet: MutableList<RTMMessage?> = mutableListOf(),
) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val view = inflater.inflate(R.layout.item_room_message, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (val item = dataSet[position]) {
            is ChatMessage -> {
                if (item.isSelf) {
                    viewHolder.rightMessageLayout.isVisible = true
                    viewHolder.leftMessageLayout.isVisible = false
                    viewHolder.noticeMessageLayout.isVisible = false

                    viewHolder.rightMessage.text = item.message
                } else {
                    viewHolder.rightMessageLayout.isVisible = false
                    viewHolder.leftMessageLayout.isVisible = true
                    viewHolder.noticeMessageLayout.isVisible = false

                    viewHolder.leftMessage.text = item.message
                    viewHolder.leftName.text = item.name
                }
            }
            is NoticeMessage -> {
                viewHolder.rightMessageLayout.isVisible = false
                viewHolder.leftMessageLayout.isVisible = false
                viewHolder.noticeMessageLayout.isVisible = true

                viewHolder.noticeMessage.run {
                    text = if (item.ban) {
                        context.getString(R.string.message_muted)
                    } else {
                        context.getString(R.string.message_unmuted)
                    }
                }
            }
        }
    }

    override fun getItemCount() = dataSet.size

    fun setMessages(it: List<RTMMessage>) {
        dataSet.clear()
        dataSet.addAll(it)
        notifyDataSetChanged()
    }

    fun addMessagesAtHead(msgs: List<RTMMessage>) {
        dataSet.addAll(0, msgs)
        notifyItemRangeInserted(0, msgs.size)
    }

    fun addMessagesAtTail(msgs: List<RTMMessage>) {
        dataSet.addAll(msgs)
        notifyItemRangeInserted(dataSet.size - msgs.size, msgs.size);
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val leftMessageLayout: View = view.findViewById(R.id.leftMessageLayout)
        val leftName: TextView = view.findViewById(R.id.name)
        val leftMessage: TextView = view.findViewById(R.id.leftMessage)
        val rightMessageLayout: View = view.findViewById(R.id.rightMessageLayout)
        val rightMessage: TextView = view.findViewById(R.id.rightMessage)
        val noticeMessageLayout: View = view.findViewById(R.id.noticeMessageLayout)
        val noticeMessage: TextView = view.findViewById(R.id.noticeMessage)
    }
}
