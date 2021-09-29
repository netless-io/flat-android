package io.agora.flat.ui.activity.play

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.util.inflate

class ApplianceAdapter(
    private val dataSet: List<ApplianceItem>,
) : RecyclerView.Adapter<ApplianceAdapter.ViewHolder>() {

    private var currentAppliance: ApplianceItem? = null
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(viewGroup.inflate(R.layout.item_toolbox_appliance, viewGroup, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]

        viewHolder.appliance.setImageResource(item.drawableRes)
        viewHolder.itemView.isSelected = (item == currentAppliance)
        viewHolder.itemView.setOnClickListener {
            if (item != ApplianceItem.OTHER_CLEAR) {
                currentAppliance = item
            }
            onItemClickListener?.onApplianceClick(item)
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = dataSet.size

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun setCurrentAppliance(appliance: ApplianceItem) {
        currentAppliance = appliance
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appliance: ImageView = view.findViewById(R.id.appliance)
    }

    fun interface OnItemClickListener {
        fun onApplianceClick(item: ApplianceItem)
    }
}