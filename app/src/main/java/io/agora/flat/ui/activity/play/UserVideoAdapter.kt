package io.agora.flat.ui.activity.play

import android.annotation.SuppressLint
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.common.rtc.AudioVolumeInfo
import io.agora.flat.data.model.RoomUser
import io.agora.flat.databinding.ItemClassRtcVideoBinding
import io.agora.flat.ui.manager.WindowsDragManager
import io.agora.flat.ui.view.FlatDrawables
import io.agora.flat.util.dp2px
import io.agora.flat.util.loadAvatarAny
import kotlin.math.abs


/**
 * 用户视频区域
 */
class UserVideoAdapter(
    private val dataSet: MutableList<RoomUser> = mutableListOf(),
    private val viewModel: ClassRoomViewModel,
    private val windowsDragManager: WindowsDragManager,
) : RecyclerView.Adapter<UserVideoAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemClassRtcVideoBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context
        val itemData = dataSet[position]
        val onboard = windowsDragManager.isOnBoard(itemData.userUUID)
        val avatar = if (itemData.isJoined) {
            itemData.avatarURL
        } else {
            itemData.avatarURL.ifEmpty { R.drawable.img_user_left }
        }

        val binding = viewHolder.binding
        binding.avatar.loadAvatarAny(avatar)
        binding.avatarLayout.isVisible = (!itemData.isJoined || !itemData.videoOpen) && !onboard
        binding.userOffline.isVisible = !itemData.isJoined

        binding.micClosed.isVisible = !itemData.audioOpen
        binding.username.text = itemData.name
        binding.onboardUsername.text = itemData.name
        binding.onboardLayout.isVisible = onboard
        binding.switchCamera.setImageDrawable(FlatDrawables.createCameraDrawable(context))
        binding.switchCamera.isSelected = itemData.videoOpen
        binding.switchMic.setImageDrawable(FlatDrawables.createMicDrawable(context))
        binding.switchMic.isSelected = itemData.audioOpen

        if (viewModel.isOwner()) {
            binding.allowDraw.isVisible = !itemData.isOwner && !itemData.allowDraw
            binding.forbidDraw.isVisible = !itemData.isOwner && itemData.allowDraw
            binding.sendReward.isVisible = !itemData.isOwner
            binding.restoreUserWindow.isVisible = itemData.isOwner
            binding.muteMicAll.isVisible = itemData.isOwner
        } else {
            binding.allowDraw.isVisible = false
            binding.forbidDraw.isVisible = false
            binding.sendReward.isVisible = false
            binding.restoreUserWindow.isVisible = false
            binding.muteMicAll.isVisible = false
        }

        binding.switchCamera.setOnClickListener {
            onItemListener?.onSwitchCamera(itemData.userUUID, !binding.switchCamera.isSelected)
        }
        binding.switchMic.setOnClickListener {
            onItemListener?.onSwitchMic(itemData.userUUID, !binding.switchMic.isSelected)
        }
        binding.allowDraw.setOnClickListener {
            onItemListener?.onAllowDraw(itemData.userUUID, true)
        }
        binding.forbidDraw.setOnClickListener {
            onItemListener?.onAllowDraw(itemData.userUUID, false)
        }
        binding.sendReward.setOnClickListener {
            onItemListener?.onSendReward(itemData.userUUID)
        }
        binding.restoreUserWindow.setOnClickListener {
            onItemListener?.onRestoreUserWindow()
        }
        binding.muteMicAll.setOnClickListener {
            onItemListener?.onMuteAll()
        }

        if (itemData.isJoined && !onboard) {
            viewHolder.binding.videoContainer.let { windowsDragManager.setupUserVideo(it, itemData.rtcUID) }
        } else {
            viewHolder.binding.videoContainer.removeAllViews()
        }

        val gestureDetector = GestureDetector(viewHolder.itemView.context, object : SimpleOnGestureListener() {
            val dismissLayout = Runnable {
                binding.switchDeviceLayout.isVisible = false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                return onItemListener?.onItemDoubleClick(
                    viewHolder.binding.videoContainer,
                    itemData
                ) ?: false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!viewModel.canControlDevice(itemData.userUUID) || !itemData.isJoined) return false
                binding.switchDeviceLayout.isVisible = true
                binding.switchDeviceLayout.removeCallbacks(dismissLayout)
                binding.switchDeviceLayout.postDelayed(dismissLayout, 3000)
                return true
            }
        })

        viewHolder.itemView.setOnTouchListener(object : View.OnTouchListener {
            var lastX: Float = 0f
            var lastY: Float = 0f
            var flag: Boolean = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flag = false
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (flag) return true
                        val dx: Float = event.rawX - lastX
                        val dy: Float = event.rawY - lastY
                        if (abs(dx) >= context.dp2px(8) || abs(dy) >= context.dp2px(8)) {
                            flag = true
                            onItemListener?.onStartDrag(v, itemData)
                        }
                    }
                }

                return true
            }
        })

        viewHolder.itemView.setOnDragListener { _, event ->
            onItemListener?.onDrag(viewHolder.binding.videoContainer, viewHolder.bindingAdapterPosition, event) ?: false
        }
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].userUUID.hashCode().toLong()
    }

    override fun getItemCount() = dataSet.size

    fun updateUsers(newUsers: List<RoomUser>) {
        dataSet.clear()
        dataSet.addAll(newUsers.distinctBy { it.userUUID })
        notifyDataSetChanged()
    }

    fun getUserItem(position: Int): RoomUser {
        return dataSet[position]
    }

    fun updateVideoView(uid: Int) {
        recyclerView?.findViewHolderForItemId(uid.toLong())?.also {
            windowsDragManager.setupUserVideo((it as ViewHolder).binding.videoContainer, uid)
        }
    }

    private fun findItemIndex(uuid: String): Int {
        return dataSet.indexOfFirst { it.userUUID == uuid }
    }

    fun findContainerByUuid(uuid: String): FrameLayout? {
        val index = dataSet.indexOfFirst { it.userUUID == uuid }
        return (recyclerView?.findViewHolderForAdapterPosition(index) as? ViewHolder)?.binding?.videoContainer
    }

    class ViewHolder(val binding: ItemClassRtcVideoBinding) : RecyclerView.ViewHolder(binding.root)

    private var onItemListener: OnItemListener? = null

    fun setOnItemListener(onItemListener: OnItemListener) {
        this.onItemListener = onItemListener
    }

    fun updateItemByUuid(uuid: String) {
        val index = findItemIndex(uuid)
        if (index >= 0) notifyItemChanged(index)
    }

    fun updateVolume(speakers: List<AudioVolumeInfo>) {

    }

    interface OnItemListener {
        fun onStartDrag(view: View, user: RoomUser): Boolean

        fun onDrag(v: View, position: Int, event: DragEvent): Boolean

        fun onItemClick(view: View, position: Int)

        fun onItemDoubleClick(view: View, user: RoomUser): Boolean

        fun onSwitchCamera(userId: String, on: Boolean) {}

        fun onSwitchMic(userId: String, on: Boolean) {}

        fun onAllowDraw(userUUID: String, allow: Boolean) {}

        fun onMuteAll() {}

        fun onRestoreUserWindow() {}

        fun onSendReward(userUUID: String) {}
    }
}

