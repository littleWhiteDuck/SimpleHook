package me.simpleHook.recyclerview.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.recyclerview.adapter.ImExportAdapter.ViewHolder
import me.simpleHook.bean.ConfigItem
import me.simpleHook.ui.view.main.ShareItemView

class ImExportAdapter(private val onCheckedChange: (Boolean, Int) -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    private var dataList: List<ConfigItem> = ArrayList()

    inner class ViewHolder(shareItemView: ShareItemView) : RecyclerView.ViewHolder(shareItemView) {
        val tvInformation = shareItemView.information
        val checkBox = shareItemView.checkBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val shareItemView = ShareItemView(parent.context)
        val holder = ViewHolder(shareItemView)
        holder.itemView.setOnClickListener {
            val position = holder.itemView.getTag(R.id.item_in_export_config_position) as Int
            holder.checkBox.isChecked = !dataList[position].isChecked
            dataList[position].isChecked = !dataList[position].isChecked
            onCheckedChange(dataList[position].isChecked, position)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val configItem = dataList[position]
        holder.itemView.setTag(R.id.item_in_export_config_position, position)
        holder.apply {
            configItem.apply {
                tvInformation.text = appConfig.appName
                checkBox.isChecked = isChecked
            }
        }
    }

    override fun getItemCount() = dataList.size

    fun setDataList(list: List<ConfigItem>) {
        dataList = list
    }
}