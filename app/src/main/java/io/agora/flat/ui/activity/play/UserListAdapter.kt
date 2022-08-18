package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.data.model.RtcUser

/**
 * 用户列表
 */
class UserListAdapter(
    private val viewModel: ClassRoomViewModel,
    private val dataSet: MutableList<RtcUser> = mutableListOf(),
) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val view = inflater.inflate(R.layout.item_room_user_list, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val itemData = dataSet[position]

        viewHolder.username.text = itemData.name

        // state
        viewHolder.state.run {
            val classState = viewModel.state.value ?: return
            when {
                classState.isCreator(itemData.userUUID) -> {
                    setText(R.string.room_class_userlist_state_teacher)
                    setTextColor(ContextCompat.getColor(context, R.color.flat_text_secondary))
                }
                itemData.isSpeak -> {
                    setText(R.string.room_class_userlist_state_speaking)
                    setTextColor(ContextCompat.getColor(context, R.color.flat_light_green))
                }
                itemData.isRaiseHand -> {
                    setText(R.string.room_class_userlist_state_handup)
                    setTextColor(ContextCompat.getColor(context, R.color.flat_blue))
                }
                else -> {
                    text = ""
                }
            }
        }

        // handup
        viewHolder.handup.run {
            val state = viewModel.state.value ?: return
            if (state.isOwner) {
                when {
                    state.isCreator(itemData.userUUID) -> {
                        isVisible = false
                    }
                    itemData.isSpeak -> {
                        isVisible = true
                        setImageResource(R.drawable.ic_room_userlist_handup_close)
                        setOnClickListener {
                            viewModel.closeSpeak(itemData.userUUID)
                        }
                    }
                    itemData.isRaiseHand -> {
                        isVisible = true
                        setImageResource(R.drawable.ic_room_userlist_handup_agree)
                        setOnClickListener {
                            viewModel.acceptRaiseHand(itemData.userUUID)
                        }
                    }
                    else -> isVisible = false
                }
            } else {
                if (itemData.isSpeak && state.isCurrentUser(itemData.userUUID)) {
                    isVisible = true
                    setImageResource(R.drawable.ic_room_userlist_handup_close)
                    setOnClickListener {
                        viewModel.closeSpeak(itemData.userUUID)
                    }
                } else {
                    isVisible = false
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].rtcUID.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setData(data: List<RtcUser>) {
        dataSet.clear()
        dataSet.addAll(data.distinctBy { it.rtcUID })
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val handup: ImageView = view.findViewById(R.id.handup)
        val username: TextView = view.findViewById(R.id.username)
        val state: TextView = view.findViewById(R.id.state)
    }
}
