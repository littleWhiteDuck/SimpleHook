package me.simpleHook.adapter


import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.ui.view.config.ConfigItemView
import me.simpleHook.util.marquee

class ConfigAdapter(
    private val onClick: (position: Int) -> Unit, private val onLongClick: (position: Int) -> Unit
) : ListAdapter<ConfigBean, ConfigAdapter.ViewHolder>(MethodConfigDiffCallback) {


    inner class ViewHolder(itemView: ConfigItemView) : RecyclerView.ViewHolder(itemView) {
        val tvClassName = itemView.className
        val tvOtherName = itemView.otherName
        val tvNumber = itemView.num
        val tip = itemView.tip
    }

    @SuppressLint("ResourceType")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val configView = ConfigItemView(parent.context)
        val viewHolder = ViewHolder(configView)
        viewHolder.apply {
            tvClassName.marquee()
            tvOtherName.marquee()
            itemView.apply {
                setOnClickListener {
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    onClick(position)
                }
                setOnLongClickListener {
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    onLongClick(position)
                    true
                }
            }
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        val methodConfig = getItem(position)
        val context = holder.itemView.context
        holder.apply {
            tvClassName.text = methodConfig.className
            tvOtherName.text = methodConfig.methodName.ifEmpty { methodConfig.fieldName }
            tvNumber.text = (position + 1).toString()
            tip.tip.text = when (methodConfig.mode) {
                Constant.HOOK_RETURN -> context.getString(R.string.config_mode_tip_return_value)
                Constant.HOOK_PARAM -> context.getString(R.string.config_mode_tip_param_value)
                Constant.HOOK_STATIC_FIELD, Constant.HOOK_FIELD -> context.getString(R.string.config_mode_tip_field_value)
                Constant.HOOK_BREAK -> context.getString(R.string.config_mode_tip_break)
                Constant.HOOK_RECORD_PARAMS -> context.getString(R.string.config_mode_tip_record_param_value)
                Constant.HOOK_RECORD_RETURN -> context.getString(R.string.config_mode_tip_record_return_value)
                Constant.HOOK_RECORD_PARAMS_RETURN -> context.getString(R.string.config_mode_tip_record_param_return_value)
                else -> "未知"
            }
        }
    }

    object MethodConfigDiffCallback : DiffUtil.ItemCallback<ConfigBean>() {
        override fun areItemsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className && oldItem.methodName == newItem.methodName && oldItem.mode == newItem.mode && oldItem.params == newItem.params && oldItem.resultValues == newItem.resultValues && oldItem.fieldType == newItem.fieldType && oldItem.fieldName == newItem.fieldName
        }

        override fun areContentsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className && oldItem.methodName == newItem.methodName && oldItem.mode == newItem.mode && oldItem.params == newItem.params && oldItem.resultValues == newItem.resultValues && oldItem.fieldType == newItem.fieldType && oldItem.fieldName == newItem.fieldName
        }
    }

}