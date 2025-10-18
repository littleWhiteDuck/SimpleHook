package me.simpleHook.recyclerview.adapter

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
import me.simpleHook.R
import me.simpleHook.database.entity.RecordEntity
import me.simpleHook.extension.dp
import me.simpleHook.extension.showToast
import me.simpleHook.utils.JsonUtil
import me.simpleHook.utils.ToolUtil

class FloatRecordAdapter :
    ListAdapter<RecordEntity, FloatRecordAdapter.ViewHolder>(RecordCallback) {

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
            val recordEntity = viewHolder.itemView.getTag(R.id.item_print_position) as RecordEntity
            val message = JsonUtil.formatJson(recordEntity.record.replace("\\u003e", ">"))
            val dialog = MaterialAlertDialogBuilder(parent.context).setMessage(message)
                .setPositiveButton(parent.context.getString(R.string.record_detail_menu_copy)) { dialog, _ ->
                    ToolUtil.toClip(parent.context, message)
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
        val recordEntity = getItem(position)
        holder.itemView.setTag(R.id.item_print_position, recordEntity)
        holder.tvLog.text = recordEntity.type.name
    }


    object RecordCallback : DiffUtil.ItemCallback<RecordEntity>() {
        override fun areItemsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
            return oldItem.record == newItem.record && oldItem.id == newItem.id
        }

    }
}