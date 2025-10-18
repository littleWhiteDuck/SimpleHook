package me.simpleHook.recyclerview.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.simpleHook.R
import me.simpleHook.database.entity.RecordEntity
import me.simpleHook.extension.dp
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.utils.IconHelper
import me.simpleHook.utils.RecordType

class RecordAdapter(
    private val isType: Boolean = false,
    private val onItemClick: (RecordEntity) -> Unit,
    private val onItemLongClick: (RecordEntity) -> Unit,
    private val deleteRecord: (RecordEntity) -> Unit,
    private val markRecord: (RecordEntity) -> Unit
) : PagingDataAdapter<RecordEntity, RecordAdapter.ViewHolder>(RecordDiff) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView(parent.context)
        val viewHolder = ViewHolder(recordView)
        recordView.container.setOnClickListener {
            val recordEntity = viewHolder.itemView.getTag(R.id.item_record_position) as RecordEntity
            onItemClick(recordEntity)
        }
        recordView.container.setOnLongClickListener {
            val recordEntity = viewHolder.itemView.getTag(R.id.item_record_position) as RecordEntity
            onItemLongClick(recordEntity)
            true
        }
        recordView.mark.setOnClickListener {
            val recordEntity = viewHolder.itemView.getTag(R.id.item_record_position) as RecordEntity
            markRecord(recordEntity)
        }
        recordView.delete.setOnClickListener {
            val recordEntity = viewHolder.itemView.getTag(R.id.item_record_position) as RecordEntity
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
            title.text = recordEntity.type.name
            time.text = recordEntity.time
            readState.text =
                if (recordEntity.isRead) context.getString(R.string.record_item_status_read) else context.getString(
                    R.string.record_item_status_unread
                )
            when {
                recordEntity.isMark -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record_mark)
                }

                recordEntity.isRead -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record_read)
                }

                else -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record)
                }
            }
            if (isType && !recordEntity.type.name.startsWith("Error")) {
                Glide.with(icon).load(recordEntity.packageName).into(icon)
            } else {
                val showText = RecordType.getShowText(recordEntity.type.name)
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

    object RecordDiff : DiffUtil.ItemCallback<RecordEntity>() {
        override fun areItemsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
            return oldItem.record == newItem.record && oldItem.isRead == newItem.isRead && oldItem.isMark == newItem.isMark
        }

    }
}