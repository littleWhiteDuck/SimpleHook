package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.extension.dp
import me.simpleHook.extension.marquee
import me.simpleHook.ui.view.applist.AppItemView
import me.simpleHook.util.GlideApp

class AppListAdapter(val onItemClick: (AppItem) -> Unit) :
    ListAdapter<AppItem, AppListAdapter.ViewHolder>(AppDiffCallback) {

    inner class ViewHolder(appItem: AppItemView) : RecyclerView.ViewHolder(appItem) {
        private val containerView = appItem.containerView
        val ivIcon = containerView.icon
        val tvAppName = containerView.appName
        val tvPackageName = containerView.packageName
        val tvOtherInfo = containerView.otherInfo
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val appItemView = AppItemView(parent.context).apply {
            cardElevation = 1f.dp
            radius = 5f
        }
        val holder = ViewHolder(appItemView)
        holder.itemView.setOnClickListener {
            val appItem: AppItem = holder.itemView.getTag(R.id.item_select_position) as AppItem
            onItemClick(appItem)
        }
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = getItem(position)
        holder.itemView.setTag(R.id.item_select_position, appItem)
        holder.apply {
            appItem.apply {
                GlideApp.with(ivIcon).load(packageName).into(ivIcon)
                tvAppName.text = name
                tvAppName.marquee()
                tvPackageName.text = packageName
                tvPackageName.marquee()
                tvOtherInfo.text = "$versionName($versionCode), Target Api $targetApi"
                tvOtherInfo.marquee()
            }
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.name == newItem.name && oldItem.packageName == newItem.packageName && oldItem.versionName == newItem.versionName && oldItem.installedTime == newItem.installedTime

    }


}