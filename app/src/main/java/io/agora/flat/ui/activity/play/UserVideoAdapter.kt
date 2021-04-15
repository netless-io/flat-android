package io.agora.flat.ui.activity.play

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.flat.R
import io.agora.flat.data.model.RtcUser

class UserVideoAdapter(
    private val dataSet: MutableList<RtcUser>,
    private val rtcEngine: RtcEngine?
) :
    RecyclerView.Adapter<UserVideoAdapter.ViewHolder>() {

    private var context: Context? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        context = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_class_rtc_video, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.user.load(dataSet[position].avatarURL) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        viewHolder.videoContainer.apply {
            if (childCount >= 1) {
                return
            }

            val surfaceView = RtcEngine.CreateRendererView(context)
            viewHolder.videoContainer.addView(surfaceView)
        }
        // 设置远端视图。
        rtcEngine!!.setupRemoteVideo(
            VideoCanvas(
                viewHolder.videoContainer.getChildAt(0), VideoCanvas.RENDER_MODE_HIDDEN,
                dataSet[position].rtcUID.toInt()
            )
        )
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setDataSet(data: List<RtcUser>) {
        dataSet.clear()
//        repeat(10) {
            dataSet.addAll(data)
//        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
        val user: ImageView = view.findViewById(R.id.user)
    }
}
