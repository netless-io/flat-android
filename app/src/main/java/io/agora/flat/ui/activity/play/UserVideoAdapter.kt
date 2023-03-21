package io.agora.flat.ui.activity.play

import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import io.agora.flat.common.rtc.AudioVolumeInfo
import io.agora.flat.data.model.RoomUser
import io.agora.flat.databinding.ItemClassRtcVideoBinding
import io.agora.flat.ui.manager.WindowsDragManager
import io.agora.flat.ui.view.FlatDrawables


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

        val binding = viewHolder.binding
        binding.avatar.load(itemData.avatarURL) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        binding.teacherLeaveLy.isVisible = itemData.isLeft && itemData.isOwner
        binding.studentLeaveLy.isVisible = itemData.isLeft && !itemData.isOwner
        binding.micClosed.isVisible = !itemData.audioOpen
        binding.username.text = itemData.name
        binding.onboardUsername.text = itemData.name
        binding.onboardLayout.isVisible = onboard
        binding.videoClosedLayout.isVisible = itemData.isJoined && !itemData.videoOpen && !onboard
        binding.switchCamera.setImageDrawable(FlatDrawables.createCameraDrawable(context))
        binding.switchCamera.isSelected = itemData.videoOpen
        binding.switchMic.setImageDrawable(FlatDrawables.createMicDrawable(context))
        binding.switchMic.isSelected = itemData.audioOpen

        binding.switchCamera.setOnClickListener {
            onItemListener?.onSwitchCamera(itemData.userUUID, !binding.switchCamera.isSelected)
        }
        binding.switchMic.setOnClickListener {
            onItemListener?.onSwitchMic(itemData.userUUID, !binding.switchMic.isSelected)
        }

        if (itemData.isJoined && !onboard) {
            windowsDragManager.setupUserVideo(viewHolder.binding.videoContainer, itemData.rtcUID)
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
                if (!viewModel.canControlDevice(itemData.userUUID)) return false
                binding.switchDeviceLayout.isVisible = true
                binding.switchDeviceLayout.removeCallbacks(dismissLayout)
                binding.switchDeviceLayout.postDelayed(dismissLayout, 3000)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                onItemListener?.onItemLongPress(viewHolder.binding.videoContainer, itemData)
            }
        })
        viewHolder.itemView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

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

    fun findVideoContainerByUuid(uuid: String): FrameLayout? {
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
        fun onItemLongPress(view: View, user: RoomUser): Boolean

        fun onItemClick(view: View, position: Int)

        fun onItemDoubleClick(view: View, user: RoomUser): Boolean

        fun onDrag(v: View, position: Int, event: DragEvent): Boolean

        fun onSwitchCamera(userId: String, on: Boolean) {}

        fun onSwitchMic(userId: String, on: Boolean) {}
    }
}

