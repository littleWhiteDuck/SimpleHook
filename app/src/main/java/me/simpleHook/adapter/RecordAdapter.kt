package me.simpleHook.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.util.AppUtils

class RecordAdapter(val onItemClick: (PrintLog) -> Unit) :
    ListAdapter<PrintLog, RecordAdapter.ViewHolder>(RecordDiff) {
    inner class ViewHolder(recordView: RecordItemView) : RecyclerView.ViewHolder(recordView) {
        val title = recordView.title
        val icon = recordView.icon
        val time = recordView.desc
        val readState = recordView.readState
    }


    object RecordDiff : DiffUtil.ItemCallback<PrintLog>() {
        override fun areItemsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.log == newItem.log && oldItem.read == newItem.read
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView(parent.context)
        val viewHolder = ViewHolder(recordView)
        recordView.setOnClickListener {
            val printLog: PrintLog = viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            onItemClick(printLog)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = getItem(position)
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
        holder.itemView.setTag(R.id.item_record_position, printLog)
        holder.apply {
            title.text = logBean.type
            time.text = logBean.packageName
            icon.setImageDrawable(AppUtils.getIcon(holder.itemView.context, logBean.packageName))
            readState.text = if (printLog.read) "已读" else "未读"
        }
    }
}