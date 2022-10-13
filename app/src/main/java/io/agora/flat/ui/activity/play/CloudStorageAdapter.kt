package io.agora.flat.ui.activity.play

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.ResourceType
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.fileIcon
import io.agora.flat.util.inflate

class CloudStorageAdapter(
    private val dataSet: MutableList<CloudFile> = mutableListOf(),
) : RecyclerView.Adapter<CloudStorageAdapter.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = viewGroup.inflate(R.layout.item_room_cloud_storage, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]
        viewHolder.fileType.setImageResource(item.fileIcon())
        viewHolder.filename.text = item.fileName
        viewHolder.fileDate.text = FlatFormatter.longDate(item.createAt)
        viewHolder.fileSize.text = FlatFormatter.size(item.fileSize)
        viewHolder.add.isVisible = item.resourceType != ResourceType.Directory
        viewHolder.itemView.setOnClickListener { onItemClickListener?.onAddClick(item) }
    }

    override fun getItemCount() = dataSet.size

    fun setDataSet(data: List<CloudFile>) {
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
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val fileDate: TextView = view.findViewById(R.id.file_date)
        val fileType: ImageView = view.findViewById(R.id.file_type_image)
    }

    fun interface OnItemClickListener {
        fun onAddClick(item: CloudFile)
    }
}
