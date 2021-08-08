package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.MethodConfig
import me.simpleHook.databinding.MethodItemLayoutBinding

class MethodAdapter(private val listener: OnItemClickListener) :
    ListAdapter<MethodConfig, MethodAdapter.ViewHolder>(MethodConfigDiffCallback) {
    private lateinit var binding: MethodItemLayoutBinding

    inner class ViewHolder(binding: MethodItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(methodConfig: MethodConfig, position: Int) {
            binding.apply {
                classSimpleName.text = methodConfig.className
                methodName.text = if (methodConfig.methodName.isEmpty()) methodConfig.fieldName else methodConfig.methodName
                num.text = (position + 1).toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            MethodItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
            listener.onItemClickListener(position)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        holder.bind(getItem(position), position)
    }


    interface OnItemClickListener {
        fun onItemClickListener(position: Int)
    }

    object MethodConfigDiffCallback : DiffUtil.ItemCallback<MethodConfig>() {
        override fun areItemsTheSame(oldItem: MethodConfig, newItem: MethodConfig): Boolean {
            return oldItem.className == newItem.className &&
                    oldItem.methodName == newItem.methodName &&
                    oldItem.mode == newItem.mode &&
                    oldItem.params == newItem.params &&
                    oldItem.resultValues == oldItem.resultValues
        }

        override fun areContentsTheSame(oldItem: MethodConfig, newItem: MethodConfig): Boolean {
            return oldItem.className == newItem.className &&
                    oldItem.methodName == newItem.methodName &&
                    oldItem.mode == newItem.mode &&
                    oldItem.params == newItem.params &&
                    oldItem.resultValues == oldItem.resultValues
        }
    }

}