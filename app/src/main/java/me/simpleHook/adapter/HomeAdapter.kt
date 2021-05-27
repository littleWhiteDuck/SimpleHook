package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.databinding.MainItemLayoutBinding
import me.simpleHook.utils.AppUtils

class HomeAdapter(private val onClick: (AppConfigEntity) -> Unit,
private val onChange: (AppConfigEntity,Boolean) -> Unit) :
    ListAdapter<AppConfigEntity, HomeAdapter.ViewHolder>(AppDiffCallback) {

    private lateinit var binding: MainItemLayoutBinding

    inner class ViewHolder(binding: MainItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appConfig: AppConfigEntity) {
            binding.apply {
                appConfig.apply {
                    itemName.text = appName
                    itemDesc.text = description
                    ableSwitch.isChecked = canUse
                    itemAppIcon.setImageDrawable(
                        AppUtils.getIcon(
                            binding.root.context,
                            packageName
                        )
                    )
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = MainItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        binding.constraintLayout.setOnClickListener {
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfigEntity
            onClick(appConfig)
        }
        binding.ableSwitch.setOnCheckedChangeListener { _, isChecked ->
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfigEntity
            onChange(appConfig,isChecked)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_home_position, getItem(position))
        holder.bind(getItem(position))
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppConfigEntity>() {
        override fun areItemsTheSame(oldItem: AppConfigEntity, newItem: AppConfigEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AppConfigEntity,
            newItem: AppConfigEntity
        ): Boolean{
            return oldItem.appName == newItem.appName &&
                    oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.description == newItem.description &&
                    oldItem.config == newItem.config &&
                    oldItem.canUse == newItem.canUse
        }
    }

}