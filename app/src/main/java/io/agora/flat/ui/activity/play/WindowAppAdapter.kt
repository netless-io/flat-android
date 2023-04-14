package io.agora.flat.ui.activity.play

import android.view.*
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.data.model.WindowAppItem
import io.agora.flat.databinding.ItemWindowAppBinding


/**
 * 用户视频区域
 */
class WindowAppAdapter(
    private val dataSet: List<WindowAppItem>,
) : RecyclerView.Adapter<WindowAppAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWindowAppBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        )
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val binding = viewHolder.binding
        binding.appIcon.setImageResource(dataSet[position].icon)
        binding.appName.setText(dataSet[position].title)
        binding.root.setOnClickListener {
            onItemClickListener?.onClick(position, dataSet[position])
        }
    }

    class ViewHolder(val binding: ItemWindowAppBinding) : RecyclerView.ViewHolder(binding.root)

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun interface OnItemClickListener {
        fun onClick(position: Int, itemData: WindowAppItem)
    }
}

