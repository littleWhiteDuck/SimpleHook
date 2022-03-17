package me.simpleHook.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class MultiTypeAdapter(
    private val list: List<Any>, private val viewHolderFactory: BasicViewHolderFactory
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int =
        viewHolderFactory.getItemViewType(position, list[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicViewHolder<*> {
        return viewHolderFactory.onCreateViewHolder(
            parent, viewHolderFactory.getItemView(parent, viewType)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BasicViewHolder<*>) {
            (holder as BasicViewHolder<Any>).onBindData(position, list[position])
        }
    }


}

abstract class BasicViewHolderFactory {
    abstract fun getItemViewType(position: Int, data: Any): Int
    abstract fun getItemView(parent: ViewGroup, viewType: Int): View
    abstract fun onCreateViewHolder(parent: ViewGroup, itemView: View): BasicViewHolder<*>
}

abstract class BasicViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun onBindData(position: Int, data: T)
}

