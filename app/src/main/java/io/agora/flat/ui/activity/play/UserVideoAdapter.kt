package io.agora.flat.ui.activity.play

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import io.agora.flat.R
import io.agora.flat.data.model.RoomUser
import io.agora.flat.ui.viewmodel.RtcVideoController
import io.agora.flat.util.inflate

/**
 * 用户视频区域
 */
class UserVideoAdapter(
    private val dataSet: MutableList<RoomUser>,
    private val rtcVideoController: RtcVideoController,
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
        val view = viewGroup.inflate(R.layout.item_class_rtc_video, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val itemData = dataSet[position]

        viewHolder.avatar.load(itemData.avatarURL) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        viewHolder.teacherLeaveLy.isVisible = itemData.isLeft && itemData.isOwner
        viewHolder.studentLeaveLy.isVisible = itemData.isLeft && !itemData.isOwner
        viewHolder.micClosed.isVisible = !itemData.audioOpen

        if (itemData.isJoined && itemData.rtcUID != rtcVideoController.fullScreenUid) {
            rtcVideoController.setupUserVideo(viewHolder.videoContainer, itemData.rtcUID)
        }

        viewHolder.itemView.setOnClickListener {
            if (itemData.isLeft)
                return@setOnClickListener
            onItemClickListener?.onItemClick(position, viewHolder.videoContainer, itemData)
        }
        viewHolder.videoClosedLayout.isVisible = !itemData.isLeft && !itemData.videoOpen
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].rtcUID.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun updateUsers(newUsers: List<RoomUser>) {
        dataSet.clear()
        dataSet.addAll(newUsers.distinctBy { it.userUUID })
        notifyDataSetChanged()
    }

    fun updateVideoView(uid: Int) {
        recyclerView?.findViewHolderForItemId(uid.toLong())?.also {
            rtcVideoController.setupUserVideo((it as ViewHolder).videoContainer, uid)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
        val videoClosedLayout: FrameLayout = view.findViewById(R.id.videoClosedLayout)
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val username: TextView = view.findViewById(R.id.username)
        val micClosed: View = view.findViewById(R.id.mic_closed)
        val teacherLeaveLy: ViewGroup = view.findViewById(R.id.teacher_leave_ly)
        val studentLeaveLy: ViewGroup = view.findViewById(R.id.student_leave_ly)
    }

    var onItemClickListener: OnItemClickListener? = null

    fun interface OnItemClickListener {
        fun onItemClick(position: Int, view: ViewGroup, rtcUser: RoomUser)
    }
}
