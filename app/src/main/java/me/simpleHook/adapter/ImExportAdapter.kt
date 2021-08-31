package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.adapter.ImExportAdapter.ViewHolder
import me.simpleHook.bean.ConfigItem
import me.simpleHook.databinding.ItemImExportConfigBinding

class ImExportAdapter(private val onCheckedChange: (Boolean, Int) -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    private var dataList: List<ConfigItem> = ArrayList()

    inner class ViewHolder(binding: ItemImExportConfigBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvInformation = binding.information
        val checkBox = binding.checkBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemImExportConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val position = holder.itemView.getTag(R.id.item_in_export_config_position) as Int
            dataList[position].isChecked = isChecked
            onCheckedChange(isChecked, position)
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