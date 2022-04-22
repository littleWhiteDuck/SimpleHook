package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.ui.custom.SwipeMenuLayout
import me.simpleHook.ui.view.record.RecordItemView
import me.simpleHook.util.GlideApp
import me.simpleHook.util.IconHelper
import me.simpleHook.util.RecordType
import me.simpleHook.util.dp

class RecordAdapter(
    val isType: Boolean = false,
    val onItemClick: (PrintLog) -> Unit,
    private val deleteRecord: (PrintLog) -> Unit,
    private val markRecord: (PrintLog) -> Unit
) : PagingDataAdapter<PrintLog, RecordAdapter.ViewHolder>(RecordDiff) {
    inner class ViewHolder(recordView: CardView) : RecyclerView.ViewHolder(recordView) {
        private val swipe: SwipeMenuLayout = recordView.findViewWithTag("swipe")
        val deleteRecord: AppCompatButton = swipe.findViewWithTag("delete")
        val markRecord: AppCompatButton = swipe.findViewWithTag("mark")
        val container: RecordItemView.ContainerView = swipe.findViewWithTag("recordView")
        val title = container.title
        val icon = container.icon
        val time = container.desc
        val readState = container.tip
        var id: Int? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recordView = RecordItemView.ContainerView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        recordView.tag = "recordView"
        val swipeMenuLayout = SwipeMenuLayout(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tag = "swipe"
        }
        swipeMenuLayout.addView(recordView, 0)
        val mark = AppCompatButton(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
            ).also {
                setPadding(0, 0, 0, 0)
            }
            text = "标记"
            tag = "mark"
            setTextColor(Color.WHITE)
            setBackgroundColor("#4F9BFA".toColorInt())
        }
        val delete = AppCompatButton(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
            ).also {
                setPadding(0, 0, 0, 0)
            }
            tag = "delete"
            text = "删除"
            setTextColor(Color.WHITE)
            setBackgroundColor("#FF80AB".toColorInt())
        }
        swipeMenuLayout.addView(mark)
        swipeMenuLayout.addView(delete)
        val cardView = CardView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(5.dp, 5.dp, 5.dp, 0)
            }
            cardElevation = 1.dp.toFloat()
            radius = 5.dp.toFloat()
            addView(swipeMenuLayout)
        }
        val viewHolder = ViewHolder(cardView)
        recordView.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            onItemClick(printLog)
        }
        mark.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            markRecord(printLog)
        }
        delete.setOnClickListener {
            val printLog: PrintLog =
                viewHolder.itemView.getTag(R.id.item_record_position) as PrintLog
            deleteRecord(printLog)
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = getItem(position) ?: return
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
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

            if (printLog.isMark) {
                holder.container.setBackgroundResource(R.drawable.bg_record_mark)
            } else if (printLog.read) {
                holder.container.setBackgroundResource(R.drawable.bg_record_read)
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_record)
            }
            if (isType && !printLog.type.startsWith("Error")) {
                GlideApp.with(icon).load(logBean.packageName).into(icon)
            } else {
                val showText = RecordType.getShowText(printLog.type)
                icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            }
            markRecord.text = if (printLog.isMark) "取消标记" else "标记"
        }
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