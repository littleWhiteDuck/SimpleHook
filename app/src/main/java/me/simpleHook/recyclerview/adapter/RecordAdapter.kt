package me.simpleHook.recyclerview.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.extension.dp
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.util.GlideApp
import me.simpleHook.util.IconHelper
import me.simpleHook.util.RecordType

class RecordAdapter(
    private val isType: Boolean = false,
    private val onItemClick: (PrintLog) -> Unit,
    private val onItemLongClick: (PrintLog) -> Unit,
    private val deleteRecord: (PrintLog) -> Unit,
    private val markRecord: (PrintLog) -> Unit
) : PagingDataAdapter<PrintLog, RecordAdapter.ViewHolder>(RecordDiff) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView(parent.context)
        val viewHolder = ViewHolder(recordView)
        recordView.container.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            onItemClick(printLog)
        }
        recordView.container.setOnLongClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            onItemLongClick(printLog)
            true
        }
        recordView.mark.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            markRecord(printLog)
        }
        recordView.delete.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            deleteRecord(printLog)
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = getItem(position) ?: return
        val logBean = Json.decodeFromString<LogBean>(printLog.log)
        holder.itemView.setTag(R.id.item_record_position, printLog)
        val context = holder.itemView.context
        holder.apply {
            id = printLog.id
            title.text = logBean.type
            time.text = printLog.time
            readState.text =
                if (printLog.read) context.getString(R.string.record_item_status_read) else context.getString(
                    R.string.record_item_status_unread
                )
            when {
                printLog.isMark -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record_mark)
                }
                printLog.read -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record_read)
                }
                else -> {
                    holder.container.setBackgroundResource(R.drawable.bg_record)
                }
            }
            if (isType && !printLog.type.startsWith("Error")) {
                GlideApp.with(icon).load(logBean.packageName).into(icon)
            } else {
                val showText = RecordType.getShowText(printLog.type)
                icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            }
            markRecord.text =
                if (printLog.isMark) context.getString(R.string.cancel_mark) else context.getString(
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

    object RecordDiff : DiffUtil.ItemCallback<PrintLog>() {
        override fun areItemsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.log == newItem.log && oldItem.read == newItem.read && oldItem.isMark == newItem.isMark
        }

    }
}