package me.simpleHook.feature.record.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.simpleHook.R
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.core.extension.dp
import me.simpleHook.feature.record.ui.view.RecordItemView
import me.simpleHook.core.utils.IconHelper
import me.simpleHook.core.utils.RecordTypeUtils

class RecordAdapter(
    private val isType: Boolean = false,
    private val onItemClick: (SmallRecordEntity) -> Unit,
    private val onItemLongClick: (SmallRecordEntity) -> Unit,
    private val deleteRecord: (SmallRecordEntity) -> Unit,
    private val markRecord: (SmallRecordEntity) -> Unit
) : PagingDataAdapter<SmallRecordEntity, RecordAdapter.ViewHolder>(RecordDiff) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView(parent.context)
        val viewHolder = ViewHolder(recordView)
        recordView.container.setOnClickListener {
            val recordEntity =
                viewHolder.itemView.getTag(R.id.item_record_position) as SmallRecordEntity
            onItemClick(recordEntity)
        }
        recordView.container.setOnLongClickListener {
            val recordEntity =
                viewHolder.itemView.getTag(R.id.item_record_position) as SmallRecordEntity
            onItemLongClick(recordEntity)
            true
        }
        recordView.mark.setOnClickListener {
            val recordEntity =
                viewHolder.itemView.getTag(R.id.item_record_position) as SmallRecordEntity
            markRecord(recordEntity)
        }
        recordView.delete.setOnClickListener {
            val recordEntity =
                viewHolder.itemView.getTag(R.id.item_record_position) as SmallRecordEntity
            deleteRecord(recordEntity)
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordEntity = getItem(position) ?: return

        holder.itemView.setTag(R.id.item_record_position, recordEntity)
        val context = holder.itemView.context
        with(holder) {
            id = recordEntity.id
            title.text = recordEntity.subType
            time.text = recordEntity.time
            readState.text =
                if (recordEntity.isRead) context.getString(R.string.record_item_status_read) else context.getString(
                    R.string.record_item_status_unread
                )
            // TODO
            container.setBackgroundResource(
                when {
                    recordEntity.isMark -> R.drawable.bg_record_mark
                    recordEntity.isRead -> R.drawable.bg_record_read
                    else -> R.drawable.bg_record
                }
            )
            if (isType) {
                Glide.with(icon).load(recordEntity.packageName).into(icon)
            } else {
                val showText = RecordTypeUtils.getShowText(recordEntity.type)
                icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            }
            markRecord.text =
                if (recordEntity.isMark) context.getString(R.string.cancel_mark) else context.getString(
                    R.string.mark
                )
        }
    }

    inner class ViewHolder(recordView: RecordItemView) : RecyclerView.ViewHolder(recordView) {
        val container = recordView.container
        val title = container.title
        val icon = container.icon
        val time = container.desc
        val readState = container.tip
        val markRecord = recordView.mark
        var id: Int? = null
    }

    object RecordDiff : DiffUtil.ItemCallback<SmallRecordEntity>() {
        override fun areItemsTheSame(
            oldItem: SmallRecordEntity,
            newItem: SmallRecordEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SmallRecordEntity,
            newItem: SmallRecordEntity
        ): Boolean {
            return oldItem.type == newItem.type
                    && oldItem.subType == newItem.subType
                    && oldItem.isRead == newItem.isRead
                    && oldItem.isMark == newItem.isMark
                    && oldItem.time == newItem.time
        }

    }
}
