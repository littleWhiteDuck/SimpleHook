package me.simpleHook.ui.fragment.config

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.base.BaseBottomFragment
import me.simpleHook.ui.view.config.HookModeView

class HookModeFragment(val items: Array<String>, onItemClick: (Int) -> Unit) :
    BaseBottomFragment<RecyclerView>() {

    private val mAdapter by lazy {
        HookModeAdapter { position, _ ->
            onItemClick(position)
            dismiss()
        }
    }

    override fun initRootView(): RecyclerView {
        return RecyclerView(requireContext()).apply {
            setPadding(5.dp, 0, 5.dp, 20.dp)
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    setMargins(0, 10.dp, 0, 0)
                }
            clipToPadding = false
            layoutManager = GridLayoutManager(requireContext(), 2)

        }
    }

    override fun init() {
        mAdapter.items = items
        root.adapter = mAdapter
    }

}


class HookModeAdapter(val onClick: (Int, mode: Int) -> Unit) :
    RecyclerView.Adapter<HookModeAdapter.ViewHolder>() {
    var items: Array<String> = emptyArray()

    inner class ViewHolder(view: HookModeView) : RecyclerView.ViewHolder(view) {
        val title = view.title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val hookModeView = HookModeView(parent.context)
        hookModeView.setOnClickListener {
            val position = it.getTag(R.id.item_hook_mode) as Int
            onClick(position, 0)
        }
        hookModeView.setOnLongClickListener {
            val position = it.getTag(R.id.item_hook_mode) as Int
            onClick(position, 1)
            true
        }
        return ViewHolder(hookModeView)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = items[position]
        holder.itemView.setTag(R.id.item_hook_mode, position)
        holder.title.text = str
    }
}