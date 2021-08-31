package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.ConfigBean
import me.simpleHook.databinding.ItemConfigLayoutBinding
import me.simpleHook.util.marquee

class ConfigAdapter(private val onClick: (position :Int) -> Unit, private val onLongClick: (position:Int) -> Unit) :
    ListAdapter<ConfigBean, ConfigAdapter.ViewHolder>(MethodConfigDiffCallback) {
    private lateinit var binding: ItemConfigLayoutBinding

    inner class ViewHolder(binding: ItemConfigLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvClassName = binding.classSimpleName
        val tvMethodName = binding.methodName
        val tvNumber = binding.num
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            ItemConfigLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.apply {
            tvClassName.marquee()
            tvMethodName.marquee()
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        val methodConfig = getItem(position)
        holder.apply {
            tvClassName.text = methodConfig.className
            tvMethodName.text = if (methodConfig.methodName.isEmpty()) methodConfig.fieldName else methodConfig.methodName
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