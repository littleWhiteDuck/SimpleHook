package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.databinding.MethodItemLayoutBinding
import me.simpleHook.bean.MethodConfig

class MethodAdapter(private val listener: OnItemClickListener) : RecyclerView.Adapter<MethodAdapter.ViewHolder>() {
    private lateinit var binding: MethodItemLayoutBinding
    private var methodsList: List<MethodConfig> = ArrayList()

    inner class ViewHolder(binding: MethodItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(methodConfig: MethodConfig, position: Int) {
            binding.apply {
                classSimpleName.text = methodConfig.className
                methodName.text = methodConfig.methodName
                num.text = (position+1).toString()
            }
        }
    }

    fun setMethodList(list: List<MethodConfig>) {
        methodsList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = MethodItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
            listener.onItemClickListener(position)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        holder.bind(methodsList[position],position)
    }

    override fun getItemCount() = methodsList.size

    interface OnItemClickListener {
        fun onItemClickListener(position: Int)
    }
}