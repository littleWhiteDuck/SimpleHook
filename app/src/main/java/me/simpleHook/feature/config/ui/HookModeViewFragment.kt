package me.simpleHook.feature.config.ui

import android.graphics.Typeface
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.base.BaseBottomViewFragment
import me.simpleHook.feature.config.ui.view.HookModeView

class HookModeViewFragment(
    private val mode: Int,
    val items: Array<String>,
    onItemClick: (Int) -> Unit
) :
    BaseBottomViewFragment<RecyclerView>() {

    private val mAdapter by lazy {
        HookModeAdapter(mode) { position ->
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


class HookModeAdapter(private val mode: Int, val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<HookModeAdapter.ViewHolder>() {
    var items: Array<String> = emptyArray()

    inner class ViewHolder(view: HookModeView) : RecyclerView.ViewHolder(view) {
        val title = view.title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val hookModeView = HookModeView(parent.context)
        hookModeView.setOnClickListener {
            val position = it.getTag(R.id.item_hook_mode) as Int
            onClick(position)
        }
        return ViewHolder(hookModeView)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = items[position]
        holder.itemView.setTag(R.id.item_hook_mode, position)
        holder.title.text = str

        val modeView: HookModeView = holder.itemView as HookModeView
        val listValue = modeView.context.resources.getIntArray(R.array.config_hook_mode_item_value)

        if (listValue.indexOf(mode) == position) {
            modeView.strokeWidth = 1.dp
            modeView.title.setTypeface(null, Typeface.BOLD)
        }
    }
}