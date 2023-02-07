package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.ui.custom.PopupWindowList
import me.simpleHook.ui.view.main.AppConfigView
import me.simpleHook.util.GlideApp
import me.simpleHook.util.marquee

class HomeAdapter(
    private val menuListener: (AppConfig, menu: ContextMenu) -> Unit,
    private val onClick: (AppConfig, mode: Int) -> Unit,
    private val onChange: (AppConfig, Boolean) -> Unit,
) : ListAdapter<AppConfig, HomeAdapter.ViewHolder>(AppDiffCallback) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val appConfigView = AppConfigView(parent.context)
        val viewHolder = ViewHolder(appConfigView)
        appConfigView.container.apply {
            PopupWindowList.Builder(parent.context).watchView(this)
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
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appConfigEntity = getItem(position)
        holder.itemView.setTag(R.id.item_home_position, appConfigEntity)
        holder.apply {
            appConfigEntity.apply {
                tvAppName.text = appName
                tvAppName.marquee()
                tvConfigDesc.text = if (description.trim().isEmpty()) packageName else description
                tvConfigDesc.marquee()
                ableSwitch.isChecked = enable
                GlideApp.with(ivAppIcon).load(packageName).into(ivAppIcon)
            }
        }
    }

    inner class ViewHolder(appConfigView: AppConfigView) : RecyclerView.ViewHolder(appConfigView) {
        private val containerView = appConfigView.container
        val tvAppName = containerView.appName
        val tvConfigDesc = containerView.desc
        val ableSwitch = containerView.switch
        val ivAppIcon = containerView.icon
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppConfig>() {
        override fun areItemsTheSame(oldItem: AppConfig, newItem: AppConfig) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AppConfig, newItem: AppConfig
        ): Boolean {
            return oldItem.appName == newItem.appName && oldItem.packageName == newItem.packageName && oldItem.versionName == newItem.versionName && oldItem.description == newItem.description && oldItem.configs == newItem.configs && oldItem.enable == newItem.enable
        }
    }
}