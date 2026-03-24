package me.simpleHook.feature.dexbrowser.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.widget.ImageViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.getColorByAttr
import me.simpleHook.core.ui.custom.CircleTextDrawable
import me.simpleHook.data.DexBrowserListItem
import me.simpleHook.data.Node
import me.simpleHook.databinding.ItemDexBrowserMemberRowBinding
import me.simpleHook.databinding.ItemDexBrowserRowBinding
import me.simpleHook.databinding.ItemDexBrowserSectionBinding

class DexBrowserAdapter(
    private val onNodeClick: (Node) -> Unit,
    private val onFieldClick: (className: String, item: DexBrowserListItem.FieldItem) -> Unit,
    private val onMethodClick: (className: String, item: DexBrowserListItem.MethodItem) -> Unit
) : ListAdapter<DexBrowserListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DexBrowserListItem.SectionItem -> VIEW_TYPE_SECTION
        is DexBrowserListItem.PackageItem,
        is DexBrowserListItem.ClassItem -> VIEW_TYPE_CARD_ROW
        is DexBrowserListItem.FieldItem,
        is DexBrowserListItem.MethodItem -> VIEW_TYPE_MEMBER_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(
                ItemDexBrowserSectionBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_MEMBER_ROW -> MemberViewHolder(
                ItemDexBrowserMemberRowBinding.inflate(inflater, parent, false)
            )

            else -> CardRowViewHolder(
                ItemDexBrowserRowBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardRowViewHolder -> holder.bind(getItem(position))
            is MemberViewHolder -> holder.bind(getItem(position))
            is SectionViewHolder -> holder.bind(getItem(position) as DexBrowserListItem.SectionItem)
        }
    }

    private inner class CardRowViewHolder(
        private val binding: ItemDexBrowserRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DexBrowserListItem) {
            binding.indentSpacer.updateLayoutParams<ViewGroup.LayoutParams> {
                width = item.indentLevel * INDENT_STEP_DP.dp
            }

            when (item) {
                is DexBrowserListItem.PackageItem -> bindPackage(item)
                is DexBrowserListItem.ClassItem -> bindClass(item)
                is DexBrowserListItem.FieldItem,
                is DexBrowserListItem.MethodItem,
                is DexBrowserListItem.SectionItem -> error("Unexpected section row item")
            }
        }

        private fun bindPackage(item: DexBrowserListItem.PackageItem) {
            applyCardStyle(
                backgroundColor = if (item.expanded) binding.root.context.getColorByAttr(
                    com.google.android.material.R.attr.colorSurfaceContainerHigh
                ) else binding.root.context.getColorByAttr(
                    com.google.android.material.R.attr.colorSurfaceContainerLow
                ),
                elevationDp = if (item.expanded) 4f else 2f,
                clickable = true
            )
            binding.titleView.setTextAppearance(R.style.TextAppearance_Material3_TitleMedium)
            binding.titleView.text = item.title
            binding.titleView.setTypeface(null, Typeface.NORMAL)
            binding.subtitleView.isVisible = false
            binding.leadingIcon.setImageResource(
                if (item.expanded) R.drawable.ic_folder_open_24 else R.drawable.ic_folder_24
            )
            ImageViewCompat.setImageTintList(
                binding.leadingIcon,
                android.content.res.ColorStateList.valueOf(
                    if (item.expanded) {
                        binding.root.context.getColorByAttr(android.R.attr.colorPrimary)
                    } else {
                        binding.root.context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
                    }
                )
            )
            binding.trailingIcon.isVisible = true
            binding.trailingIcon.setImageResource(
                if (item.expanded) R.drawable.ic_expand_less_24 else R.drawable.ic_expand_more_24
            )
            binding.root.setOnClickListener { onNodeClick(item.node) }
        }

        private fun bindClass(item: DexBrowserListItem.ClassItem) {
            val hasMembers = item.fieldCount > 0 || item.methodCount > 0
            applyCardStyle(
                backgroundColor = if (item.expanded) binding.root.context.getColorByAttr(
                    com.google.android.material.R.attr.colorSecondaryContainer
                ) else binding.root.context.getColorByAttr(
                    com.google.android.material.R.attr.colorSurfaceContainerLow
                ),
                elevationDp = if (item.expanded) 4f else 2f,
                clickable = hasMembers
            )
            binding.titleView.setTextAppearance(R.style.TextAppearance_Material3_BodyLarge)
            binding.titleView.text = item.title
            binding.titleView.setTypeface(null, Typeface.NORMAL)
            binding.subtitleView.isVisible = item.expanded && hasMembers
            binding.subtitleView.text = binding.root.context.getString(
                R.string.dex_browser_field_method_count,
                item.fieldCount,
                item.methodCount
            )
            binding.leadingIcon.setImageDrawable(
                CircleTextDrawable(
                    size = 24f.dp,
                    text = if (item.isInterface) "I" else "C",
                    textColor = if (item.isInterface) INTERFACE_TEXT_COLOR else CLASS_TEXT_COLOR,
                    background = if (item.isInterface) INTERFACE_BACKGROUND_COLOR else CLASS_BACKGROUND_COLOR,
                    borderColor = if (item.isInterface) INTERFACE_TEXT_COLOR else CLASS_TEXT_COLOR
                )
            )
            binding.trailingIcon.isVisible = hasMembers
            if (hasMembers) {
                binding.trailingIcon.setImageResource(
                    if (item.expanded) R.drawable.ic_expand_less_24 else R.drawable.ic_expand_more_24
                )
                binding.root.setOnClickListener { onNodeClick(item.node) }
            } else {
                binding.root.setOnClickListener(null)
            }
        }

        private fun applyCardStyle(backgroundColor: Int, elevationDp: Float, clickable: Boolean) {
            binding.root.setCardBackgroundColor(backgroundColor)
            binding.root.cardElevation = elevationDp.dp
            binding.root.isClickable = clickable
            binding.root.isFocusable = clickable
        }
    }

    private inner class MemberViewHolder(
        private val binding: ItemDexBrowserMemberRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DexBrowserListItem) {
            binding.indentSpacer.updateLayoutParams<ViewGroup.LayoutParams> {
                width = item.indentLevel * INDENT_STEP_DP.dp + MEMBER_OFFSET_DP.dp
            }

            when (item) {
                is DexBrowserListItem.FieldItem -> bindField(item)
                is DexBrowserListItem.MethodItem -> bindMethod(item)
                is DexBrowserListItem.PackageItem,
                is DexBrowserListItem.ClassItem,
                is DexBrowserListItem.SectionItem -> error("Unexpected member row item")
            }
        }

        private fun bindField(item: DexBrowserListItem.FieldItem) {
            binding.titleView.text = item.field.name
            binding.leadingIcon.setImageDrawable(
                CircleTextDrawable(
                    size = 24f.dp,
                    text = "F",
                    textColor = FIELD_TEXT_COLOR,
                    background = FIELD_BACKGROUND_COLOR,
                    borderColor = FIELD_TEXT_COLOR
                )
            )
            binding.root.setOnClickListener { onFieldClick(item.className, item) }
        }

        private fun bindMethod(item: DexBrowserListItem.MethodItem) {
            binding.titleView.text = item.method.name
            binding.leadingIcon.setImageDrawable(
                CircleTextDrawable(
                    size = 24f.dp,
                    text = "M",
                    textColor = METHOD_TEXT_COLOR,
                    background = METHOD_BACKGROUND_COLOR,
                    borderColor = METHOD_TEXT_COLOR
                )
            )
            binding.root.setOnClickListener { onMethodClick(item.className, item) }
        }
    }

    private inner class SectionViewHolder(
        private val binding: ItemDexBrowserSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DexBrowserListItem.SectionItem) {
            binding.indentSpacer.updateLayoutParams<ViewGroup.LayoutParams> {
                width = item.indentLevel * INDENT_STEP_DP.dp + SECTION_OFFSET_DP.dp
            }
            binding.titleView.text = item.title
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DexBrowserListItem>() {
        override fun areItemsTheSame(
            oldItem: DexBrowserListItem,
            newItem: DexBrowserListItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: DexBrowserListItem,
            newItem: DexBrowserListItem
        ): Boolean = oldItem == newItem
    }

    private companion object {
        private const val VIEW_TYPE_CARD_ROW = 0
        private const val VIEW_TYPE_SECTION = 1
        private const val VIEW_TYPE_MEMBER_ROW = 2
        private const val INDENT_STEP_DP = 16
        private const val SECTION_OFFSET_DP = 36
        private const val MEMBER_OFFSET_DP = 44

        private val CLASS_TEXT_COLOR = "#204898".toColorInt()
        private val CLASS_BACKGROUND_COLOR = "#DDE8FF".toColorInt()
        private val INTERFACE_TEXT_COLOR = "#508848".toColorInt()
        private val INTERFACE_BACKGROUND_COLOR = "#DDF2DA".toColorInt()
        private val FIELD_TEXT_COLOR = "#D06800".toColorInt()
        private val FIELD_BACKGROUND_COLOR = "#FFF0DE".toColorInt()
        private val METHOD_TEXT_COLOR = "#C02020".toColorInt()
        private val METHOD_BACKGROUND_COLOR = "#FFE1E1".toColorInt()
    }
}
