package me.simpleHook.feature.config.ui.adapter

import android.view.ViewGroup
import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.feature.config.ui.adapter.ImExportAdapter.ViewHolder
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.feature.home.ui.view.ShareItemView

class ImExportAdapter(private val onCheckedChange: (Boolean, Int) -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    private var dataList: List<AppConfigItem2> = ArrayList()
    private var selectionEnabled = true

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionEnabled(enabled: Boolean) {
        selectionEnabled = enabled
        notifyDataSetChanged()
    }

    inner class ViewHolder(shareItemView: ShareItemView) : RecyclerView.ViewHolder(shareItemView) {
        val tvInformation = shareItemView.information
        val checkBox = shareItemView.checkBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val shareItemView = ShareItemView(parent.context)
        val holder = ViewHolder(shareItemView)
        holder.itemView.setOnClickListener {
            if (!selectionEnabled) return@setOnClickListener
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
        with(holder) {
            itemView.isEnabled = selectionEnabled
            checkBox.isEnabled = selectionEnabled
            tvInformation.text = configItem.appConfig.appName
            checkBox.isChecked = configItem.isChecked
        }
    }

    override fun getItemCount() = dataList.size

    fun setDataList(list: List<AppConfigItem2>) {
        dataList = list
    }
}
