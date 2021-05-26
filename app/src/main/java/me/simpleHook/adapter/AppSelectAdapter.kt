package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.AppSelectLayoutBinding

class AppSelectAdapter(private val listener:OnItemClickListener):RecyclerView.Adapter<AppSelectAdapter.ViewHolder>() {
    private var appList = ArrayList<AppItem>()
    private lateinit var binding: AppSelectLayoutBinding
    inner class ViewHolder(binding: AppSelectLayoutBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(appItem: AppItem){
            binding.appName.text = appItem.appName
            binding.apply {
                appName.text = appItem.appName
                appPackageName.text = appItem.packageName
                appVersionName.text = appItem.versionName
                appIcon.setImageDrawable(appItem.appIcon)
            }
        }
    }
    fun setAppList(list :ArrayList<AppItem>){
        appList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AppSelectLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = ViewHolder(binding)
        holder.itemView.setOnClickListener {
            val position:Int = holder.itemView.getTag(R.id.item_select_position) as Int
            listener.onItemClickListener(position)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_select_position,position)
        holder.bind(appList[position])
    }

    override fun getItemCount() = appList.size
    interface OnItemClickListener {
        fun onItemClickListener(position: Int)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

}