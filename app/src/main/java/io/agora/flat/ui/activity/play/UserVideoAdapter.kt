package io.agora.flat.ui.activity.play

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import io.agora.flat.R
import io.agora.flat.data.model.RtcUser
import io.agora.flat.ui.viewmodel.RtcVideoController

class UserVideoAdapter(
    private val dataSet: MutableList<RtcUser>,
    private val rtcVideoController: RtcVideoController
) : RecyclerView.Adapter<UserVideoAdapter.ViewHolder>() {

    init {
        setHasStableIds(true);
    }

    private var context: Context? = null
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        context = null
        this.recyclerView = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_class_rtc_video, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        var itemData = dataSet[position]

        viewHolder.profile.load(itemData.avatarURL) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        rtcVideoController.setupUserVideo(viewHolder.videoContainer, itemData.rtcUID)

        viewHolder.itemView.setOnClickListener {
            rtcVideoController.enterFullScreen(itemData.rtcUID)
            listener?.onFullScreen(position, viewHolder.videoContainer, itemData)
        }
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].rtcUID.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setDataSet(data: List<RtcUser>) {
        dataSet.clear()
        dataSet.addAll(data.distinctBy { it.rtcUID })
        notifyDataSetChanged()
    }

    fun updateVideoView(uid: Int) {
        recyclerView?.findViewHolderForItemId(uid.toLong())?.apply {
            val viewHolder = this as ViewHolder
            rtcVideoController.setupUserVideo(viewHolder.videoContainer, uid)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
        val videoClosedLayout: FrameLayout = view.findViewById(R.id.videoClosedLayout)
        val profile: ImageView = view.findViewById(R.id.profile)
        val username: TextView = view.findViewById(R.id.username)
    }

    var listener: Listener? = null
        set(value) {
            field = value
        }

    fun interface Listener {
        fun onFullScreen(position: Int, view: ViewGroup, rtcUser: RtcUser)
    }
}
