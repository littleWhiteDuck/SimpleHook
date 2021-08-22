package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.MethodConfig
import me.simpleHook.databinding.MethodItemLayoutBinding
import me.simpleHook.util.marquee

class MethodAdapter(private val listener: OnItemClickListener) :
    ListAdapter<MethodConfig, MethodAdapter.ViewHolder>(MethodConfigDiffCallback) {
    private lateinit var binding: MethodItemLayoutBinding

    inner class ViewHolder(binding: MethodItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val className = binding.classSimpleName
        val methodName = binding.methodName
        val number = binding.num
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            MethodItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.itemView.apply {
            setOnClickListener {
                val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                listener.onItemClickListener(position)
            }
            setOnLongClickListener {
                val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                listener.onItemLongClickListener(position)
                true
            }
        }
        viewHolder.apply {
            className.marquee()
            methodName.marquee()
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        val methodConfig = getItem(position)
        holder.apply {
            className.text = methodConfig.className
            methodName.text = if (methodConfig.methodName.isEmpty()) methodConfig.fieldName else methodConfig.methodName
            number.text = (position + 1).toString()
        }
    }


    interface OnItemClickListener {
        fun onItemClickListener(position: Int)
        fun onItemLongClickListener(position: Int)
    }

    object MethodConfigDiffCallback : DiffUtil.ItemCallback<MethodConfig>() {
        override fun areItemsTheSame(oldItem: MethodConfig, newItem: MethodConfig): Boolean {
            return oldItem.className == newItem.className &&
                    oldItem.methodName == newItem.methodName &&
                    oldItem.mode == newItem.mode &&
                    oldItem.params == newItem.params &&
                    oldItem.resultValues == newItem.resultValues &&
                    oldItem.fieldType == newItem.fieldType &&
                    oldItem.fieldName == newItem.fieldName
        }

        override fun areContentsTheSame(oldItem: MethodConfig, newItem: MethodConfig): Boolean {
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