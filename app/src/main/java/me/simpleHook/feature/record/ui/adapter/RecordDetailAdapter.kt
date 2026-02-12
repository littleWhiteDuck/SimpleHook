package me.simpleHook.feature.record.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import me.simpleHook.data.record.RecordDetailItem
import me.simpleHook.databinding.ItemRecordSingleValueBinding
import me.simpleHook.core.utils.ToolUtil

class RecordDetailAdapter(val onExpandClick: (RecordDetailItem) -> Unit) :
    ItemViewBinder<RecordDetailItem, RecordDetailAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ViewHolder {
        val binding = ItemRecordSingleValueBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        item: RecordDetailItem
    ) {
        with(item) {
            holder.tvTitle.text = title
            holder.tvContent.text = content
        }
        with(holder) {
            btExpand.setOnClickListener{
                onExpandClick(item)
            }
            btCopy.setOnClickListener {
                ToolUtil.toClip(holder.itemView.context, item.content)
            }
        }
    }

    class ViewHolder(binding: ItemRecordSingleValueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.title
        val btExpand = binding.expandContent
        val btCopy = binding.copyContent
        val tvContent = binding.content
    }
}
