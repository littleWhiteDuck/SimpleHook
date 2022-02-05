package me.simpleHook.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.RecordSummary
import me.simpleHook.ui.custom.CircleTextDrawable
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.RecordType
import me.simpleHook.util.dp

class RecordSummaryAdapter(val onClick: (RecordSummary) -> Unit) :
    ListAdapter<RecordSummary, RecordSummaryAdapter.ViewHolder>(RecordDiffCallback) {

    inner class ViewHolder(itemView: RecordItemView) : RecyclerView.ViewHolder(itemView) {
        val container = itemView.container
        val title = container.title
        val desc = container.desc
        val tvCount = container.tip
        val icon = container.icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordSummaryView = RecordItemView(parent.context)
        recordSummaryView.setOnClickListener {
            val recordSummary = it.getTag(R.id.item_record_summary) as RecordSummary
            onClick(recordSummary)
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
                    title.text = AppUtils.getAppName(holder.icon.context, packageName)
                    desc.text = packageName
                    tvCount.text = count.toString()
                    icon.setImageDrawable(AppUtils.getIcon(holder.itemView.context, packageName))
                } else {
                    title.text = type
                    tvCount.text = count.toString()
                    val showText = RecordType.getShowText(type)
                    icon.setImageDrawable(CircleTextDrawable(40f.dp, showText))
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