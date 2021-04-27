package io.agora.flat.ui.activity.play

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R

class ColorAdapter(
    private val dataSet: List<ColorItem>,
) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    private var currentColor: IntArray? = null
    private var context: Context? = null
    private var onItemClickListener: OnItemClickListener? = null

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
                .inflate(R.layout.item_toolbox_color, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]

        viewHolder.color.setImageResource(item.drawableRes)
        viewHolder.itemView.isSelected = (item.color.contentEquals(currentColor))
        viewHolder.itemView.setOnClickListener {
            currentColor = dataSet[position].color
            onItemClickListener?.onColorSelected(item)
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

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

    interface OnItemClickListener {
        fun onColorSelected(item: ColorItem)
    }
}
