package me.simpleHook.recyclerview.adapter

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.simpleHook.R
import me.simpleHook.constant.Constant
import me.simpleHook.data.AppConfigItem
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.extension.marquee
import me.simpleHook.ui.view.main.AppConfigView

class HomeAdapter(
    private val menuListener: (AppConfig, menu: ContextMenu) -> Unit,
    private val onClick: (AppConfig, mode: Int) -> Unit,
    private val onChange: (AppConfig, Boolean) -> Unit,
    private val onDrag: (holder: RecyclerView.ViewHolder) -> Unit
) : ListAdapter<AppConfigItem, HomeAdapter.ViewHolder>(AppDiffCallback) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val appConfigView = AppConfigView(parent.context)
        val viewHolder = ViewHolder(appConfigView)
        appConfigView.container.apply {
            setOnClickListener {
                val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
                onClick(appConfig, Constant.HOME_ITEM_CLICK_NORMAL)
            }
            setOnCreateContextMenuListener { menu, _, _ ->
                val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
                menuListener(appConfig, menu)
            }
        }
        appConfigView.editConfig.setOnClickListener {
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
            onClick(appConfig, Constant.HOME_ITEM_CLICK_EDIT)
            appConfigView.smoothClose(delayMills = 500)
        }
        appConfigView.shareConfig.setOnClickListener {
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
            onClick(appConfig, Constant.HOME_ITEM_CLICK_COPY)
            appConfigView.smoothClose()
        }
        appConfigView.deleteConfig.setOnClickListener {
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
            onClick(appConfig, Constant.HOME_ITEM_CLICK_DELETE)
        }
        viewHolder.ableSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if (switchView.isPressed) {
                val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
                onChange(appConfig, isChecked)
            }
        }
        viewHolder.dragImage.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onDrag(viewHolder)
            }
            false
        }
        return viewHolder
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appConfigBean = getItem(position)
        holder.itemView.setTag(R.id.item_home_position, appConfigBean.appConfig)
        with(holder) {
            with(appConfigBean.appConfig) {
                tvAppName.text = appName
                tvAppName.marquee()
                tvConfigDesc.text = if (description.trim().isEmpty()) packageName else description
                tvConfigDesc.marquee()
                ableSwitch.isChecked = enable
                Glide.with(ivAppIcon).load(packageName).into(ivAppIcon)
            }
            if (appConfigBean.drag) {
                holder.ableSwitch.isVisible = false
                holder.dragImage.isVisible = true
                (holder.itemView as AppConfigView).isSwipeEnable = false
            } else {
                holder.ableSwitch.isVisible = true
                holder.dragImage.isVisible = false
                (holder.itemView as AppConfigView).isSwipeEnable = true
            }
        }
    }

    inner class ViewHolder(appConfigView: AppConfigView) : RecyclerView.ViewHolder(appConfigView) {
        private val containerView = appConfigView.container
        val tvAppName = containerView.appName
        val tvConfigDesc = containerView.desc
        val ableSwitch = containerView.switch
        val ivAppIcon = containerView.icon
        val dragImage = containerView.dragImage
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppConfigItem>() {
        override fun areItemsTheSame(oldItem: AppConfigItem, newItem: AppConfigItem) =
            oldItem.appConfig.id == newItem.appConfig.id

        override fun areContentsTheSame(
            oldItem: AppConfigItem, newItem: AppConfigItem
        ): Boolean {
            val oldItemConfig = oldItem.appConfig
            val newItemConfig = newItem.appConfig
            return oldItem.drag == newItem.drag && oldItemConfig.appName == newItemConfig.appName && oldItemConfig.packageName == newItemConfig.packageName && oldItemConfig.versionName == newItemConfig.versionName && oldItemConfig.description == newItemConfig.description && oldItemConfig.configs == newItemConfig.configs && oldItemConfig.enable == newItemConfig.enable
        }
    }
}