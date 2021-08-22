package me.simpleHook.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter.ViewHolder
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ItemAssistLayoutBinding
import me.simpleHook.util.AppUtils
import me.simpleHook.util.marquee

class AssistAdapter(private val onClick: (AssistConfig) -> Unit,
private val onLongClick: (AssistConfig) -> Unit) :
    ListAdapter<AssistConfig, ViewHolder>(AssistDiff) {

    inner class ViewHolder(binding: ItemAssistLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val appName = binding.appName
        val appIcon = binding.appIcon
        val versionName = binding.versionName
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
        val binding =
            ItemAssistLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        holder.appName.marquee()
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
            appName.text = assistConfig.appName
            appIcon.setImageDrawable(AppUtils.getIcon(itemView.context, assistConfig.packageName))
            versionName.text =
                AppUtils.getAppVersionName(itemView.context, assistConfig.packageName)
        }
    }
}