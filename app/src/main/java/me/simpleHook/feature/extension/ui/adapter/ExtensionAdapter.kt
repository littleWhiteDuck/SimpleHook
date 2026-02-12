package me.simpleHook.feature.extension.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.simpleHook.R
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.feature.extension.ui.adapter.ExtensionAdapter.ViewHolder
import me.simpleHook.feature.home.ui.view.AssistItemView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.extension.marquee

class ExtensionAdapter(
    private val onClick: (ExtensionConfigEntity) -> Unit,
    private val onLongClick: (ExtensionConfigEntity) -> Unit
) : ListAdapter<ExtensionConfigEntity, ViewHolder>(AssistDiff) {

    inner class ViewHolder(assistItemView: AssistItemView) :
        RecyclerView.ViewHolder(assistItemView) {
        private val containerView = assistItemView.containerView
        val tvAppName = containerView.appName
        val ivAppIcon = containerView.icon
        val tvVersionName = containerView.versionName
    }

    object AssistDiff : DiffUtil.ItemCallback<ExtensionConfigEntity>() {
        override fun areItemsTheSame(
            oldItem: ExtensionConfigEntity,
            newItem: ExtensionConfigEntity
        ) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ExtensionConfigEntity,
            newItem: ExtensionConfigEntity
        ) =
            oldItem.enable == newItem.enable && oldItem.appName == newItem.appName && oldItem.config == newItem.config

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val assistItemView = AssistItemView(parent.context)
        val holder = ViewHolder(assistItemView)
        holder.tvAppName.marquee()
        with(holder.itemView) {
            setOnClickListener {
                val extConfigEntity = it.getTag(R.id.item_extension_config) as ExtensionConfigEntity
                onClick(extConfigEntity)
            }
            setOnLongClickListener {
                val extConfigEntity = it.getTag(R.id.item_extension_config) as ExtensionConfigEntity
                onLongClick(extConfigEntity)
                true
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val extConfigEntity = getItem(position)
        holder.itemView.setTag(R.id.item_extension_config, extConfigEntity)
        with(holder) {
            tvAppName.text = extConfigEntity.appName
            Glide.with(ivAppIcon).load(extConfigEntity.packageName).into(ivAppIcon)
            tvVersionName.text =
                AppUtil.getAppVersionName(extConfigEntity.packageName)
        }
    }
}
