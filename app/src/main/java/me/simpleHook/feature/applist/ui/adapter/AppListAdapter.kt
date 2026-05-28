package me.simpleHook.feature.applist.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.simpleHook.R
import me.simpleHook.data.AppListItem
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.marquee
import me.simpleHook.feature.applist.ui.view.AppItemView

class AppListAdapter(val onItemClick: (AppListItem) -> Unit) :
    ListAdapter<AppListItem, AppListAdapter.ViewHolder>(AppDiffCallback) {

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
            val appListItem: AppListItem = holder.itemView.getTag(R.id.item_select_position) as AppListItem
            onItemClick(appListItem)
        }
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = getItem(position)
        holder.itemView.setTag(R.id.item_select_position, appItem)
        with(holder) {
          with(appItem) {
              Glide.with(ivIcon).load(packageName).into(ivIcon)
              tvAppName.text = name.ifEmpty { packageName }
              tvAppName.marquee()
              tvPackageName.text = packageName
              tvPackageName.marquee()
              tvOtherInfo.text = ivIcon.context.getString(
                  R.string.app_list_other_info_format,
                  versionName,
                  versionCode,
                  targetApi
              )
              tvOtherInfo.marquee()
          }
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppListItem>() {
        override fun areItemsTheSame(oldItem: AppListItem, newItem: AppListItem): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppListItem, newItem: AppListItem): Boolean =
            oldItem == newItem

    }


}
