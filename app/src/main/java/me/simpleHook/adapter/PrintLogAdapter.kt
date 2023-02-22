package me.simpleHook.adapter

import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.util.*

class PrintLogAdapter : ListAdapter<PrintLog, PrintLogAdapter.ViewHolder>(RecordCallback) {

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
            val printLog = viewHolder.itemView.getTag(R.id.item_print_position) as PrintLog
            val message = JsonUtil.formatJson(printLog.log.replace("\\u003e", ">"))
            val dialog = MaterialAlertDialogBuilder(parent.context).setMessage(message)
                .setPositiveButton(parent.context.getString(R.string.record_detail_menu_copy)) { dialog, _ ->
                    ToolUtils.toClip(parent.context, message)
                    parent.context.showToast(parent.context.getString(R.string.copied))
                    dialog.dismiss()
                }.setNegativeButton(itemView.context.getString(R.string.dialog_cancel), null)
                .create()
            if (Build.VERSION.SDK_INT >= 26) {
                dialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION") dialog.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
            dialog.show()
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = getItem(position)
        holder.itemView.setTag(R.id.item_print_position, printLog)
        val logBean = Json.decodeFromString<LogBean>(printLog.log)
        holder.tvLog.text = logBean.type
    }


    object RecordCallback : DiffUtil.ItemCallback<PrintLog>() {
        override fun areItemsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PrintLog, newItem: PrintLog): Boolean {
            return oldItem.log == newItem.log && oldItem.id == newItem.id
        }

    }
}