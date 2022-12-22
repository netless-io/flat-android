package io.agora.flat.ui.activity.play

import android.R.attr.state_enabled
import android.R.attr.state_selected
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.item_room_user_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]

        holder.username.text = item.name
        holder.avatar.load(item.avatarURL) {
            crossfade(true)
            placeholder(R.drawable.ic_class_room_user_avatar)
        }
        holder.onStageSwitch.isChecked = item.isOnStage
        holder.onStageSwitch.setOnCheckedChangeListener { it, isChecked ->
            if (it.isPressed) {
                viewModel.updateOnStage(item.userUUID, isChecked)
            }
        }
        holder.allowDrawSwitch.isChecked = item.allowDraw
        holder.allowDrawSwitch.setOnCheckedChangeListener { it, isChecked ->
            if (it.isPressed) {
                viewModel.updateAllowDraw(item.userUUID, isChecked)
            }
        }
        holder.cameraSwitch.isSelected = item.videoOpen
        holder.cameraSwitch.setOnClickListener {
            val target = !it.isSelected
            viewModel.enableVideo(target, item.userUUID)
        }
        holder.micSwitch.isSelected = item.audioOpen
        holder.micSwitch.setOnClickListener {
            val target = !it.isSelected
            viewModel.enableAudio(target, item.userUUID)
        }
        holder.inRaiseHand.isVisible = item.isRaiseHand
        holder.noRaiseHand.isVisible = !item.isRaiseHand

        val context = holder.itemView.context
        if (viewModel.isOwner()) {
            if (viewModel.isOnStageAllowable()) {
                holder.agreeHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_blue_6))
            } else {
                holder.agreeHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_gray_6))
            }
            holder.agreeHandUp.setOnClickListener {
                if (viewModel.isOnStageAllowable()) {
                    viewModel.acceptRaiseHand(item.userUUID)
                }
            }
            holder.cancelHandUp.setOnClickListener {
                viewModel.cancelRaiseHand(item.userUUID)
            }
        } else {
            holder.agreeHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_blue_3))
            holder.cancelHandUp.setTextColor(ContextCompat.getColor(context, R.color.flat_blue_3))
        }

        if (viewModel.isOwner()) {
            holder.onStageSwitch.isEnabled = item.isOnStage
            holder.allowDrawSwitch.isEnabled = item.isOnStage
            holder.cameraSwitch.isEnabled = item.isOnStage && item.videoOpen
            holder.micSwitch.isEnabled = item.isOnStage && item.audioOpen
        } else {
            holder.onStageSwitch.isEnabled = item.isOnStage && viewModel.isSelf(item.userUUID)
            holder.allowDrawSwitch.isEnabled = false
            holder.cameraSwitch.isEnabled = item.isOnStage && viewModel.isSelf(item.userUUID)
            holder.micSwitch.isEnabled = item.isOnStage && viewModel.isSelf(item.userUUID)
        }
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
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val username: TextView = view.findViewById(R.id.username)
        val onStageSwitch: SwitchCompat = view.findViewById(R.id.switch_on_stage)
        val allowDrawSwitch: SwitchCompat = view.findViewById(R.id.switch_allow_draw)
        val cameraSwitch: ImageView = view.findViewById(R.id.switch_camera)
        val micSwitch: ImageView = view.findViewById(R.id.switch_mic)
        val agreeHandUp: TextView = view.findViewById(R.id.agree_handup)
        val cancelHandUp: TextView = view.findViewById(R.id.cancel_handup)
        val inRaiseHand: View = view.findViewById(R.id.in_raise_hand)
        val noRaiseHand: View = view.findViewById(R.id.no_raise_hand)

        init {
            cameraSwitch.setImageDrawable(createCameraDrawable())
            micSwitch.setImageDrawable(createMicDrawable())
        }

        private fun createCameraDrawable(): Drawable {
            val context = itemView.context

            val cameraDrawable = StateListDrawable()
            val cameraOn = context.getDrawable(R.drawable.ic_class_room_camera_on)
            val cameraOnDisable = context.getDrawable(R.drawable.ic_class_room_camera_on)?.apply {
                setTint(ContextCompat.getColor(context, R.color.flat_gray_3))
            }
            val cameraOff = context.getDrawable(R.drawable.ic_class_room_camera_off)
            val cameraOffDisable = context.getDrawable(R.drawable.ic_class_room_camera_off)?.apply {
                setTint(ContextCompat.getColor(context, R.color.flat_gray_3))
            }
            cameraDrawable.addState(intArrayOf(state_enabled, state_selected), cameraOn)
            cameraDrawable.addState(intArrayOf(-state_enabled, state_selected), cameraOnDisable)
            cameraDrawable.addState(intArrayOf(state_enabled, -state_selected), cameraOff)
            cameraDrawable.addState(intArrayOf(-state_enabled, -state_selected), cameraOffDisable)
            cameraDrawable.addState(intArrayOf(), cameraOffDisable)

            return cameraDrawable
        }

        private fun createMicDrawable(): Drawable {
            val context = itemView.context

            val micDrawable = StateListDrawable()
            val micOn = context.getDrawable(R.drawable.ic_class_room_mic_on)
            val micOnDisable = context.getDrawable(R.drawable.ic_class_room_mic_on)?.apply {
                setTint(ContextCompat.getColor(context, R.color.flat_gray_3))
            }
            val micOff = context.getDrawable(R.drawable.ic_class_room_mic_off)
            val micOffDisable = context.getDrawable(R.drawable.ic_class_room_mic_off)?.apply {
                setTint(ContextCompat.getColor(context, R.color.flat_gray_3))
            }

            micDrawable.addState(intArrayOf(state_enabled, state_selected), micOn)
            micDrawable.addState(intArrayOf(-state_enabled, state_selected), micOnDisable)
            micDrawable.addState(intArrayOf(state_enabled, -state_selected), micOff)
            micDrawable.addState(intArrayOf(-state_enabled, -state_selected), micOffDisable)
            micDrawable.addState(intArrayOf(), micOffDisable)

            return micDrawable
        }
    }
}
