package me.simpleHook.adapter


import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.ConfigBean
import me.simpleHook.ui.view.config.ConfigItemView
import me.simpleHook.util.marquee

class ConfigAdapter(
    private val onClick: (position: Int) -> Unit,
    private val onLongClick: (position: Int) -> Unit
) :
    ListAdapter<ConfigBean, ConfigAdapter.ViewHolder>(MethodConfigDiffCallback) {


    inner class ViewHolder(itemView: ConfigItemView) :
        RecyclerView.ViewHolder(itemView) {
        val tvClassName = itemView.className
        val tvOtherName = itemView.otherName
        val tvNumber = itemView.num
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
        holder.apply {
            tvClassName.text = methodConfig.className
            tvOtherName.text =
                methodConfig.methodName.ifEmpty { methodConfig.fieldName }
            tvNumber.text = (position + 1).toString()
        }
    }

    object MethodConfigDiffCallback : DiffUtil.ItemCallback<ConfigBean>() {
        override fun areItemsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className &&
                    oldItem.methodName == newItem.methodName &&
                    oldItem.mode == newItem.mode &&
                    oldItem.params == newItem.params &&
                    oldItem.resultValues == newItem.resultValues &&
                    oldItem.fieldType == newItem.fieldType &&
                    oldItem.fieldName == newItem.fieldName
        }

        override fun areContentsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className &&
                    oldItem.methodName == newItem.methodName &&
                    oldItem.mode == newItem.mode &&
                    oldItem.params == newItem.params &&
                    oldItem.resultValues == newItem.resultValues &&
                    oldItem.fieldType == newItem.fieldType &&
                    oldItem.fieldName == newItem.fieldName
        }
    }

}