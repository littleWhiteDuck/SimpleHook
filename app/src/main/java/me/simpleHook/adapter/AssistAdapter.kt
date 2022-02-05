package me.simpleHook.adapter


import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter.ViewHolder
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.ui.custom.CircleTextDrawable
import me.simpleHook.ui.view.main.AssistItemView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.dp
import me.simpleHook.util.marquee

class AssistAdapter(
    private val onClick: (AssistConfig) -> Unit,
    private val onLongClick: (AssistConfig) -> Unit
) :
    ListAdapter<AssistConfig, ViewHolder>(AssistDiff) {

    inner class ViewHolder(assistItemView: AssistItemView) :
        RecyclerView.ViewHolder(assistItemView) {
        private val containerView = assistItemView.containerView
        val tvAppName = containerView.appName
        val ivAppIcon = containerView.icon
        val tvVersionName = containerView.versionName
    }

    object AssistDiff : DiffUtil.ItemCallback<AssistConfig>() {
        override fun areItemsTheSame(oldItem: AssistConfig, newItem: AssistConfig) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AssistConfig, newItem: AssistConfig) =
            oldItem.allSwitch == newItem.allSwitch
                    && oldItem.appName == newItem.appName
                    && oldItem.config == newItem.config

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val assistItemView = AssistItemView(parent.context)
        val holder = ViewHolder(assistItemView)
        holder.tvAppName.marquee()
        holder.itemView.apply {
            setOnClickListener {
                val assistConfig = it.getTag(R.id.item_assist_config) as AssistConfig
                onClick(assistConfig)
            }
            setOnLongClickListener {
                val assistConfig = it.getTag(R.id.item_assist_config) as AssistConfig
                onLongClick(assistConfig)
                true
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val assistConfig = getItem(position)
        holder.itemView.setTag(R.id.item_assist_config, assistConfig)
        holder.apply {
            tvAppName.text = assistConfig.appName
            if (assistConfig.appName == "默认配置" && assistConfig.packageName == "默认配置") {
                ivAppIcon.setImageDrawable(CircleTextDrawable(50f.dp, "默认"))
                tvVersionName.text = "用于添加其他应用时默认开启哪些选项"
            } else {
                ivAppIcon.setImageDrawable(
                    AppUtils.getIcon(
                        itemView.context,
                        assistConfig.packageName
                    )
                )
                tvVersionName.text =
                    AppUtils.getAppVersionName(itemView.context, assistConfig.packageName)
            }
        }
    }
}