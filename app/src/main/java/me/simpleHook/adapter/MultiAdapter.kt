package me.simpleHook.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView


class MultiTypeAdapter(private val list: List<Any>,
                       private val viewHolderFactory: BasicViewHolderFactory): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int = viewHolderFactory.getLayoutResId(position, list[position])

    override fun onCreateViewHolder(parent: ViewGroup, layoutResId: Int): BasicViewHolder<*> {
        val inflater = LayoutInflater.from(parent.context)
        return viewHolderFactory.onCreateViewHolder(inflater, parent, layoutResId)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BasicViewHolder<*>) {
            (holder as BasicViewHolder<Any>).onBindData(position, list[position])
        }
    }
}

abstract class BasicViewHolderFactory {
    @LayoutRes
    abstract fun getLayoutResId(position: Int, data: Any): Int
    abstract fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup, @LayoutRes layoutResId: Int): BasicViewHolder<*>
}

abstract class BasicViewHolder<T>(itemView: View): RecyclerView.ViewHolder(itemView) {
    abstract fun onBindData(position: Int, data: T)
}

