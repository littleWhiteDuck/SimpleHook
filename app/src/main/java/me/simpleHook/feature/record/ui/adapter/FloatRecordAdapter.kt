package me.simpleHook.feature.record.ui.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.core.extension.dp

class FloatRecordAdapter(private val onItemClick: (SmallRecordEntity) -> Unit) :
    ListAdapter<SmallRecordEntity, FloatRecordAdapter.ViewHolder>(RecordCallback) {

    inner class ViewHolder(itemView: AppCompatTextView) : RecyclerView.ViewHolder(itemView) {
        val tvLog = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            AppCompatTextView(ContextThemeWrapper(parent.context, R.style.text_view_item)).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(15.dp, 5.dp, 15.dp, 5.dp)
                val typedValue = TypedValue()
                parent.context.theme.resolveAttribute(
                    R.attr.selectableItemBackground, typedValue, true
                )
                val attribute = intArrayOf(R.attr.selectableItemBackground)
                val typedArray =
                    parent.context.theme.obtainStyledAttributes(typedValue.resourceId, attribute)
                background = typedArray.getDrawable(0)
                setTextColor(Color.WHITE)
            }
        val viewHolder = ViewHolder(itemView)
        viewHolder.itemView.setOnClickListener {
            val recordEntity =
                viewHolder.itemView.getTag(R.id.item_print_position) as? SmallRecordEntity ?: return@setOnClickListener
            onItemClick(recordEntity)
        }
        return viewHolder
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordEntity = getItem(position)
        holder.itemView.setTag(R.id.item_print_position, recordEntity)
        holder.tvLog.text = recordEntity.getDisplayTitle()
    }


    object RecordCallback : DiffUtil.ItemCallback<SmallRecordEntity>() {
        override fun areItemsTheSame(oldItem: SmallRecordEntity, newItem: SmallRecordEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SmallRecordEntity,
            newItem: SmallRecordEntity
        ): Boolean {
            return oldItem == newItem
        }

    }
}
