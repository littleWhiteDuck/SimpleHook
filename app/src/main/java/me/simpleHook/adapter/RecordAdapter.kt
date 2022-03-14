package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.util.IconHelper
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.util.RecordType
import me.simpleHook.util.dp

class RecordAdapter(val isType: Boolean = false, val onItemClick: (PrintLog) -> Unit) :
    ListAdapter<PrintLog, RecordAdapter.ViewHolder>(RecordDiff) {
    inner class ViewHolder(recordView: RecordItemView) : RecyclerView.ViewHolder(recordView) {
        val container = recordView.container
        val title = container.title
        val icon = container.icon
        val time = container.desc
        val readState = container.tip
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView(parent.context)
        val viewHolder = ViewHolder(recordView)
        recordView.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            onItemClick(printLog)
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = getItem(position)
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
        holder.itemView.setTag(R.id.item_record_position, printLog)
        holder.apply {
            title.text = logBean.type
            time.text = printLog.time
            readState.text = if (printLog.read) "已读" else "未读"
            if (isType) {
                icon.setImageDrawable(
                    IconHelper.getAppIcon(
                        holder.itemView.context,
                        logBean.packageName
                    )
                )
            } else {
                val showText = RecordType.getShowText(printLog.type)
                icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            }
        }
    }


    object RecordDiff : DiffUtil.ItemCallback<PrintLog>() {
        override fun areItemsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.log == newItem.log && oldItem.read == newItem.read
        }

    }
}