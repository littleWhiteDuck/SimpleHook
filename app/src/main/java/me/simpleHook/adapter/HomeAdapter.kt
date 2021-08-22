package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.custom.PopupWindowList
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.MainItemLayoutBinding
import me.simpleHook.util.AppUtils
import me.simpleHook.util.marquee

class HomeAdapter(
    private val onClick: (AppConfig) -> Unit,
    private val onChange: (AppConfig, Boolean) -> Unit,
    private val onLongClick: (AppConfig) -> Unit
) :
    ListAdapter<AppConfig, HomeAdapter.ViewHolder>(AppDiffCallback) {

    inner class ViewHolder(binding: MainItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val itemName = binding.itemName
        val itemDesc = binding.itemDesc

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val ableSwitch = binding.ableSwitch
        val itemAppIcon = binding.itemAppIcon
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            MainItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        binding.constraintLayout.apply {
            PopupWindowList.Builder(parent.context).watchView(this)
            setOnClickListener {
                val appConfig =
                    viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
                onClick(appConfig)
            }
            setOnLongClickListener {
                val appConfig =
                    viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
                onLongClick(appConfig)
                true
            }
        }
        binding.ableSwitch.setOnCheckedChangeListener { _, isChecked ->
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfig
            onChange(appConfig, isChecked)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appConfigEntity = getItem(position)
        holder.itemView.setTag(R.id.item_home_position, appConfigEntity)
        holder.apply {
            appConfigEntity.apply {
                itemName.text = appName
                itemName.marquee()
                itemDesc.text = if (description.trim().isEmpty()) packageName else description
                itemDesc.marquee()
                ableSwitch.isChecked = canUse
                itemAppIcon.setImageDrawable(AppUtils.getIcon(holder.itemView.context, packageName))
            }
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppConfig>() {
        override fun areItemsTheSame(oldItem: AppConfig, newItem: AppConfig) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AppConfig,
            newItem: AppConfig
        ): Boolean {
            return oldItem.appName == newItem.appName &&
                    oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.description == newItem.description &&
                    oldItem.config == newItem.config &&
                    oldItem.canUse == newItem.canUse
        }
    }
}