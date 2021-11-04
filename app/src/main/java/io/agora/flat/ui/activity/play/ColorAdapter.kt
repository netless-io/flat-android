package io.agora.flat.ui.activity.play

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.util.inflate

class ColorAdapter(
    private val dataSet: List<ColorItem>,
) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    private var currentColor: IntArray? = null
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(viewGroup.inflate(R.layout.item_toolbox_color, viewGroup, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]

        viewHolder.color.setImageResource(item.drawableRes)
        viewHolder.itemView.isSelected = (item.color.contentEquals(currentColor))
        viewHolder.itemView.setOnClickListener {
            currentColor = item.color
            onItemClickListener?.onColorSelected(item)
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = dataSet.size

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun setCurrentColor(color: IntArray?) {
        currentColor = color
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val color: ImageView = view.findViewById(R.id.color)
    }

    fun interface OnItemClickListener {
        fun onColorSelected(item: ColorItem)
    }
}
