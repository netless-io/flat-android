package io.agora.flat.ui.activity.play

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.agora.flat.R
import io.agora.flat.data.model.RoomUser
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.util.inflate


/**
 * 快速接受举手列表
 */
class AcceptHandupAdapter(
    private val viewModel: ClassRoomViewModel,
    private val dataSet: MutableList<RoomUser> = mutableListOf(),
) : RecyclerView.Adapter<AcceptHandupAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_room_accept_handup, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]

        holder.username.text = item.name
        holder.avatar.load(item.avatarURL) {
            crossfade(true)
            placeholder(R.drawable.ic_class_room_user_avatar)
        }
        val context = holder.itemView.context
        if (viewModel.isOnStageAllowable()) {
            holder.agreeHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_blue_6))
        } else {
            holder.agreeHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_gray_6))
        }
        holder.agreeHandUp.setOnClickListener {
            if (viewModel.isOnStageAllowable()) {
                viewModel.acceptRaiseHand(item.userUUID)
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_ACCEPT_HANDUP, false)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].rtcUID.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setData(users: List<RoomUser>) {
        dataSet.clear()
        dataSet.addAll(users)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val username: TextView = view.findViewById(R.id.username)
        val agreeHandUp: TextView = view.findViewById(R.id.agree_handup)
    }
}
