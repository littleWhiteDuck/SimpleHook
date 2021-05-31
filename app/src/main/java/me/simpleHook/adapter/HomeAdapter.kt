package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.XPopup
import me.simpleHook.R
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.databinding.MainItemLayoutBinding
import me.simpleHook.utils.AppUtils

class HomeAdapter(
    private val onClick: (AppConfigEntity) -> Unit,
    private val onChange: (AppConfigEntity, Boolean) -> Unit,
    private val onLongClick: (AppConfigEntity, XPopup.Builder) -> Unit
) :
    ListAdapter<AppConfigEntity, HomeAdapter.ViewHolder>(AppDiffCallback) {

    inner class ViewHolder(binding: MainItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val itemName = binding.itemName
        val itemDesc = binding.itemDesc
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val ableSwitch = binding.ableSwitch
        val itemAppIcon = binding.itemAppIcon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MainItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        binding.constraintLayout.apply {
            setOnClickListener {
                val appConfig =
                    viewHolder.itemView.getTag(R.id.item_home_position) as AppConfigEntity
                onClick(appConfig)
            }
            val builder = XPopup.Builder(context)
                .hasShadowBg(false)
                .watchView(this)
            setOnLongClickListener {
                val appConfig =
                    viewHolder.itemView.getTag(R.id.item_home_position) as AppConfigEntity
                onLongClick(appConfig, builder)
                true
            }
        }
        binding.ableSwitch.setOnCheckedChangeListener { _, isChecked ->
            val appConfig = viewHolder.itemView.getTag(R.id.item_home_position) as AppConfigEntity
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
                itemDesc.text = description
                ableSwitch.isChecked = canUse
                itemAppIcon.setImageDrawable(AppUtils.getIcon(holder.itemView.context, packageName))
            }
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppConfigEntity>() {
        override fun areItemsTheSame(oldItem: AppConfigEntity, newItem: AppConfigEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AppConfigEntity,
            newItem: AppConfigEntity
        ): Boolean {
            return oldItem.appName == newItem.appName &&
                    oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.description == newItem.description &&
                    oldItem.config == newItem.config &&
                    oldItem.canUse == newItem.canUse
        }
    }
    companion object{
        private var instance:HomeAdapter? = null
        @Synchronized
        fun getHomeAdapter( onClick: (AppConfigEntity) -> Unit, onChange: (AppConfigEntity, Boolean) -> Unit, onLongClick: (AppConfigEntity, XPopup.Builder) -> Unit) = instance?.let {
            it
        }?: HomeAdapter(onClick,onChange,onLongClick).also {
            instance = it
        }
    }



}