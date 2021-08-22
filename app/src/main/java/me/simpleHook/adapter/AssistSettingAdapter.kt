package me.simpleHook.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AssistGroup
import me.simpleHook.bean.AssistItem

private const val TITLE = 0
private const val ITEM = 1

class AssistSettingAdapter(
    private val groups: List<AssistGroup>,
    private val onClick: (Boolean, String) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle = itemView as TextView
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val tvControl: TextView = itemView.findViewById(R.id.control)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TITLE) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_assist_setting_title, parent, false)
            TitleViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_assist_setting_item, parent, false)
            val holder = ItemViewHolder(itemView)
            holder.itemView.setOnClickListener {
                val assistItem = it.getTag(R.id.item_assist_setting) as AssistItem
                assistItem.isChecked = !assistItem.isChecked
                holder.tvControl.apply {
                    assistItem.apply {
                        when {
                            other.isNotEmpty() -> {
                                onClick(false, tag)
                            }
                            isChecked -> {
                                text = "已开启"
                                setTextColor(Color.parseColor("#FF03DAC5"))
                                onClick(true, tag)
                            }
                            else -> {
                                text = "未开启"
                                setTextColor(Color.parseColor("#aaaaaa"))
                                onClick(false, tag)
                            }
                        }
                    }

                }
            }
            holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var itemCount = -1
        for (group in groups) {
            itemCount++
            if (itemCount == position) {
                val titleHolder = holder as TitleViewHolder
                titleHolder.tvTitle.text = group.title
            }
            for (item in group.items) {
                itemCount++
                if (itemCount == position) {
                    val itemHolder = holder as ItemViewHolder
                    itemHolder.apply {
                        itemView.setTag(R.id.item_assist_setting, item)
                        tvTitle.text = item.title
                        when {
                            item.other.isNotEmpty() -> {
                                tvControl.text = item.other
                            }
                            item.isChecked -> {
                                tvControl.text = "已开启"
                                tvControl.setTextColor(Color.parseColor("#FF03DAC5"))
                            }
                            else -> {
                                tvControl.text = "未开启"
                                tvControl.setTextColor(Color.parseColor("#aaaaaa"))
                            }
                        }

                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        var itemCount = -1
        for (i in groups.indices) {
            itemCount++
            if (itemCount == position) {
                return TITLE
            }
            for (j in groups[i].items.indices) {
                itemCount++
                if (itemCount == position) {
                    return ITEM
                }
            }
        }
        return super.getItemViewType(position)
    }

    override fun getItemCount(): Int {
        var itemCount = 0
        for (i in groups.indices) {
            itemCount += groups.size + 1
        }
        return itemCount
    }
}