package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.data.model.CloudStorageFile

class CloudStorageAdapter(
    private val dataSet: MutableList<CloudStorageFile> = mutableListOf(),
) : RecyclerView.Adapter<CloudStorageAdapter.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(viewGroup.context);
        val view = inflate.inflate(R.layout.item_room_cloud_storage, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]

        viewHolder.filename.text = item.fileName
        viewHolder.add.setOnClickListener {
            onItemClickListener?.onAddClick(item)
        }

        viewHolder.itemView.setOnClickListener {
            onItemClickListener?.onAddClick(item)
        }
    }

    override fun getItemCount() = dataSet.size

    fun setDataSet(data: List<CloudStorageFile>) {
        dataSet.clear()
        dataSet.addAll(data)
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filename: TextView = view.findViewById(R.id.filename)
        val add: ImageView = view.findViewById(R.id.add)
    }

    fun interface OnItemClickListener {
        fun onAddClick(item: CloudStorageFile)
    }
}
