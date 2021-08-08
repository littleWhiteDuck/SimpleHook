package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.ItemSelectAppBinding

class AppSelectAdapter:ListAdapter<AppItem,AppSelectAdapter.ViewHolder>(AppDiffCallback) {
    private lateinit var listener:OnItemClickListener
    private lateinit var binding: ItemSelectAppBinding
    inner class ViewHolder(binding: ItemSelectAppBinding):RecyclerView.ViewHolder(binding.root){
        val ivIcon = binding.ivIcon
        val tvAppName = binding.tvAppName
        val tvPackageName = binding.tvPackageName
        val tvInstallTime = binding.tvInstallTime
        val tvVersionName = binding.tvVersionName
    }
    fun setOnClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemSelectAppBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = ViewHolder(binding)
        holder.itemView.setOnClickListener {
            val appItem:AppItem = holder.itemView.getTag(R.id.item_select_position) as AppItem
            listener.onItemClickListener(appItem)
        }
        return holder
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = getItem(position)
        holder.itemView.setTag(R.id.item_select_position,appItem)
        holder.apply {
            appItem.apply {
                ivIcon.setImageDrawable(icon)
                tvAppName.text = name
                tvPackageName.text = packageName
                tvInstallTime.text = installedTime
                tvVersionName.text = versionName
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClickListener(appItem: AppItem)
    }

    companion object{
        private var instance1:AppSelectAdapter? =null
        @Synchronized
        fun getAppSelectAdapter1():AppSelectAdapter = instance1?.let {
            it
        }?: AppSelectAdapter().also {
            instance1 = it
        }
        private var instance2:AppSelectAdapter? =null
        @Synchronized
        fun getAppSelectAdapter2():AppSelectAdapter = instance2?.let {
            it
        }?: AppSelectAdapter().also {
            instance2 = it
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.packageName == newItem.packageName

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.name == newItem.name &&
                    oldItem.icon == newItem.icon &&
                    oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.installedTime == newItem.installedTime

    }


}