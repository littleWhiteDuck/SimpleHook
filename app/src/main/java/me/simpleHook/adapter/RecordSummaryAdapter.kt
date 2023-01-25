package me.simpleHook.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.RecordSummary
import me.simpleHook.ui.view.record.RecordSummaryItemView
import me.simpleHook.util.*

class RecordSummaryAdapter(
    val onClick: (RecordSummary) -> Unit, val onDeleteClick: (RecordSummary) -> Unit
) : ListAdapter<RecordSummary, RecordSummaryAdapter.ViewHolder>(RecordDiffCallback) {

    inner class ViewHolder(itemView: RecordSummaryItemView) : RecyclerView.ViewHolder(itemView) {
        val container = itemView.container
        val title = container.title
        val desc = container.desc
        val tvCount = container.tip
        val icon = container.icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordSummaryView = RecordSummaryItemView(parent.context)
        recordSummaryView.container.setOnClickListener {
            val recordSummary = recordSummaryView.getTag(R.id.item_record_summary) as RecordSummary
            onClick(recordSummary)
        }
        recordSummaryView.delete.setOnClickListener {
            val recordSummary = recordSummaryView.getTag(R.id.item_record_summary) as RecordSummary
            onDeleteClick(recordSummary)
        }
        return ViewHolder(recordSummaryView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordSummary = getItem(position)
        holder.itemView.setTag(R.id.item_record_summary, recordSummary)
        val packMode = recordSummary.packageName.isNotEmpty()
        holder.apply {
            recordSummary.apply {
                if (packMode) {
                    title.text =
                        if (packageName.startsWith("error")) "Hook Error" else AppUtils.getAppName(
                            holder.icon.context, packageName
                        )
                    desc.text = packageName
                    tvCount.text = count.toString()
                    if (packageName.startsWith("error.")) {
                        icon.setImageDrawable(IconHelper.getTextIcon(text = "Error"))
                    } else {
                        GlideApp.with(icon).load(packageName).into(icon)
                    }
                } else {
                    title.text = if (type.startsWith("Error")) "Hook Error" else type
                    tvCount.text = count.toString()
                    val showText = RecordType.getShowText(type)
                    icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
                    desc.visibility = View.GONE
                }
            }

        }
    }

    object RecordDiffCallback : DiffUtil.ItemCallback<RecordSummary>() {
        override fun areItemsTheSame(oldItem: RecordSummary, newItem: RecordSummary): Boolean {
            return oldItem.packageName == newItem.packageName && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: RecordSummary, newItem: RecordSummary): Boolean {
            return oldItem.packageName == newItem.packageName && oldItem.type == newItem.type && oldItem.count == newItem.count
        }
    }
}