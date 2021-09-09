package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.ui.view.AppItemView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.dp
import me.simpleHook.util.marquee

class AppListAdapter : ListAdapter<AppItem, AppListAdapter.ViewHolder>(AppDiffCallback) {
    private lateinit var listener: OnItemClickListener

    inner class ViewHolder(appItem: AppItemView) : RecyclerView.ViewHolder(appItem) {
        private val containerView = appItem.containerView
        val ivIcon = containerView.icon
        val tvAppName = containerView.appName
        val tvPackageName = containerView.packageName
        val tvOtherInfo = containerView.otherInfo
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val appItemView = AppItemView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.setMargins(5.dp, 5.dp, 5.dp, 0)
            }
            cardElevation = 3.dp.toFloat()
            radius = 5.dp.toFloat()
        }
        val holder = ViewHolder(appItemView)
        holder.itemView.setOnClickListener {
            val appItem: AppItem = holder.itemView.getTag(R.id.item_select_position) as AppItem
            listener.onItemClickListener(appItem)
        }
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = getItem(position)
        holder.itemView.setTag(R.id.item_select_position, appItem)
        holder.apply {
            appItem.apply {
                ivIcon.setImageDrawable(AppUtils.getIcon(itemView.context, packageName))
                tvAppName.text = name
                tvAppName.marquee()
                tvPackageName.text = packageName
                tvPackageName.marquee()
                tvOtherInfo.text = "$versionName, $installedTime, $targetApi"
                tvOtherInfo.marquee()
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClickListener(appItem: AppItem)
    }

    companion object {
        private var instance1: AppListAdapter? = null

        @Synchronized
        fun getAppSelectAdapter1(): AppListAdapter = instance1 ?: AppListAdapter().also {
            instance1 = it
        }

        private var instance2: AppListAdapter? = null

        @Synchronized
        fun getAppSelectAdapter2(): AppListAdapter = instance2 ?: AppListAdapter().also {
            instance2 = it
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.name == newItem.name &&
                    oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.installedTime == newItem.installedTime

    }


}