package me.simpleHook.feature.dexbrowser.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.getColorByAttr
import me.simpleHook.data.DexCandidate
import me.simpleHook.databinding.ItemDexBrowserCandidateBinding

class DexCandidateAdapter(
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<DexCandidateAdapter.ViewHolder>() {

    private var items: List<DexCandidate> = emptyList()
    private var selectedIds: Set<String> = emptySet()

    init {
        setHasStableIds(true)
    }

    fun submit(items: List<DexCandidate>, selectedIds: Set<String>) {
        this.items = items
        this.selectedIds = selectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDexBrowserCandidateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], items[position].id in selectedIds)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()

    inner class ViewHolder(
        private val binding: ItemDexBrowserCandidateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DexCandidate, checked: Boolean) {
            binding.checkBox.isChecked = checked
            binding.titleView.text = item.displayName
            binding.sourceView.text = binding.root.context.getString(
                R.string.dex_browser_select_dialog_source,
                item.sourceName
            )
            binding.root.setCardBackgroundColor(
                if (checked) {
                    binding.root.context.getColorByAttr(
                        com.google.android.material.R.attr.colorSecondaryContainer
                    )
                } else {
                    binding.root.context.getColorByAttr(
                        com.google.android.material.R.attr.colorSurfaceContainerLow
                    )
                }
            )
            binding.root.cardElevation = if (checked) 4f.dp else 0f
            binding.root.setOnClickListener { onToggle(item.id) }
            binding.checkBox.setOnClickListener { onToggle(item.id) }
        }
    }
}
