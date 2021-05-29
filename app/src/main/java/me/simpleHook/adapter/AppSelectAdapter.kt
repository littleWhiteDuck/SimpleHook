package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.AppSelectLayoutBinding

class AppSelectAdapter:ListAdapter<AppItem,AppSelectAdapter.ViewHolder>(AppDiffCallback) {
    private lateinit var listener:OnItemClickListener
    private lateinit var binding: AppSelectLayoutBinding
    inner class ViewHolder(binding: AppSelectLayoutBinding):RecyclerView.ViewHolder(binding.root){
        val appNameText = binding.appName
        val packageNameText = binding.appPackageName
        val versionNameText = binding.appVersionName
        val appIconImage = binding.appIcon
    }
    fun setOnClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AppSelectLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
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
                appNameText.text = appName
                packageNameText.text = packageName
                versionNameText.text = versionName
                appIconImage.setImageDrawable(appIcon)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClickListener(appItem: AppItem)
    }

    companion object{
        private var instance:AppSelectAdapter? =null
        fun getAppSelectAdapter():AppSelectAdapter = instance?.let {
            it
        }?: AppSelectAdapter().also {
            instance = it
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.packageName == newItem.packageName

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.appName == newItem.appName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.appIcon == newItem.appIcon

    }


}