package io.agora.flat.ui.activity.play

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.agora.flat.R
import io.agora.flat.data.model.RoomUser
import io.agora.flat.util.inflate

/**
 * 用户列表
 */
class UserListAdapter(
    private val viewModel: ClassRoomViewModel,
    private val dataSet: MutableList<RoomUser> = mutableListOf(),
) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var handUpDrawable: Drawable? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = viewGroup.inflate(R.layout.item_room_user_list, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val itemData = dataSet[position]

        viewHolder.username.text = itemData.name

        // state
        viewHolder.state.run {
            when {
                itemData.isOwner -> {
                    if (itemData.isLeft) {
                        setTextAndColor(R.string.room_class_userlist_state_teacher_left, R.color.flat_red)
                    } else {
                        setTextAndColor(
                            R.string.room_class_userlist_state_teacher,
                            R.color.flat_day_night_text_secondary
                        )
                    }
                }
                itemData.isSpeak -> {
                    if (itemData.isLeft) {
                        setTextAndColor(R.string.room_class_userlist_state_speaking_left, R.color.flat_red)
                    } else {
                        setTextAndColor(R.string.room_class_userlist_state_speaking, R.color.flat_light_green)
                    }
                }
                itemData.isRaiseHand -> {
                    setTextAndColor(R.string.room_class_userlist_state_handup, R.color.flat_blue)
                }
                else -> {
                    text = ""
                }
            }
        }

        // handup
        viewHolder.handup.run {
            if (viewModel.isOwner()) {
                when {
                    itemData.isOwner -> {
                        isVisible = false
                    }
                    itemData.isSpeak -> {
                        isVisible = true
                        setImageResource(R.drawable.ic_room_user_list_handup_close)
                        setOnClickListener {
                            viewModel.closeSpeak(itemData.userUUID)
                        }
                    }
                    itemData.isRaiseHand -> {
                        isVisible = true
                        if (handUpDrawable == null) {
                            handUpDrawable = ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_room_userlist_handup_agree
                            )
                        }
                        if (viewModel.isOnStageAllowable()) {
                            handUpDrawable?.setTint(ContextCompat.getColor(context, R.color.flat_blue_6))
                        } else {
                            handUpDrawable?.setTint(ContextCompat.getColor(context, R.color.flat_gray))
                        }
                        setImageDrawable(handUpDrawable)
                        setOnClickListener {
                            if (viewModel.isOnStageAllowable()) {
                                viewModel.acceptRaiseHand(itemData.userUUID)
                            }
                        }
                    }
                    else -> isVisible = false
                }
            } else {
                if (itemData.isSpeak && viewModel.isSelf(itemData.userUUID)) {
                    isVisible = true
                    setImageResource(R.drawable.ic_room_user_list_handup_close)
                    setOnClickListener {
                        viewModel.closeSpeak(itemData.userUUID)
                    }
                } else {
                    isVisible = false
                }
            }
        }

        viewHolder.avatar.load(itemData.avatarURL) {
            crossfade(true)
            placeholder(R.drawable.ic_class_room_user_avatar)
        }
    }

    private fun TextView.setTextAndColor(@StringRes textId: Int, @ColorRes colorId: Int) {
        setText(textId)
        setTextColor(ContextCompat.getColor(context, colorId))
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].rtcUID.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setData(data: List<RoomUser>) {
        dataSet.clear()
        dataSet.addAll(data.distinctBy { it.rtcUID })
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val handup: ImageView = view.findViewById(R.id.handup)
        val username: TextView = view.findViewById(R.id.username)
        val state: TextView = view.findViewById(R.id.state)
        val avatar: ImageView = view.findViewById(R.id.avatar)
    }
}
