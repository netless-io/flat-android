package io.agora.flat.ui.view

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.data.model.LoadState
import io.agora.flat.util.inflate

class FooterAdapter : RecyclerView.Adapter<FooterAdapter.FooterViewHolder>() {
    private var loadState: LoadState = LoadState.NotLoading(false)
    private var onFooterClickListener: OnFooterClickListener? = null

    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text: TextView = view.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        return FooterViewHolder(parent.inflate(R.layout.recycler_item_footer, parent, false))
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        val context = holder.itemView.context
        val state = loadState
        when (state) {
            LoadState.Loading -> holder.text.text = context.getString(R.string.loaded_loading)
            is LoadState.Error -> holder.text.text = context.getString(R.string.loaded_retry)
            is LoadState.NotLoading -> {
                if (state.end) {
                    holder.text.text = context.getString(R.string.loaded_all)
                } else {
                    holder.text.text = context.getString(R.string.loaded_retry)
                }
            }
        }

        if (state is LoadState.Error || state is LoadState.NotLoading && !state.end) {
            holder.itemView.setOnClickListener {
                onFooterClickListener?.onClick()
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    fun updateState(loadState: LoadState) {
        this.loadState = loadState
        notifyItemChanged(0)
    }

    override fun getItemCount(): Int = 1

    fun setOnFooterClickListener(listener: OnFooterClickListener) {
        onFooterClickListener = listener
    }

    fun interface OnFooterClickListener {
        fun onClick()
    }
}