package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.agora.flat.R

class SceneAdapter : RecyclerView.Adapter<SceneAdapter.ViewHolder>() {
    private val dataSet: MutableList<SceneItem> = ArrayList()
    private var current: Int = 0
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_scene_preview, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]
        val index = position

        viewHolder.itemView.isSelected = (index == current)
        viewHolder.itemView.setOnClickListener {
            current = index
            onItemClickListener?.onItemClick(index, item)
            notifyDataSetChanged()
        }
        // 页码显示值为索引+1
        viewHolder.index.text = (index + 1).toString()
        viewHolder.preview.load(item.preview)
    }

    override fun getItemCount() = dataSet.size

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun setDataSetAndIndex(dataSet: List<SceneItem>, index: Int) {
        this.dataSet.clear()
        this.dataSet.addAll(dataSet)
        current = index.coerceAtMost(dataSet.size - 1)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val preview: ImageView = view.findViewById(R.id.preview)
        val index: TextView = view.findViewById(R.id.index)
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int, item: SceneItem)
    }
}
