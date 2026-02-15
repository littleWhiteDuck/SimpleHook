package me.simpleHook.feature.record.ui.delegate

import android.content.Context
import android.view.ContextMenu
import com.bumptech.glide.Glide
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.R
import me.simpleHook.data.RecordShowPack
import me.simpleHook.feature.record.ui.view.RecordPackItemView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.IconHelper

class RecordPackDelegate(
    private val onClick: (RecordShowPack) -> Unit,
    private val onDeleteClick: (RecordShowPack) -> Unit,
    private val onCreateContextMenu: (RecordShowPack, ContextMenu) -> Unit
) : ViewDelegate<RecordShowPack, RecordPackItemView>() {
    override fun onBindView(view: RecordPackItemView, item: RecordShowPack) {
        view.container.apply {
            if (item.packageName.startsWith("error.")) {
                title.text = context.getString(R.string.record_type_error)
                icon.setImageDrawable(
                    IconHelper.getTextIcon(text = context.getString(R.string.common_error_short))
                )
            } else {
                Glide.with(icon).load(item.packageName).into(icon)
                title.text = AppUtil.getAppName(item.packageName)
            }

            desc.text = item.packageName
            tip.text = item.count.toString()
            setOnClickListener { onClick(item) }
            setOnCreateContextMenuListener { menu, _, _ ->
                onCreateContextMenu(item, menu)
            }
        }
        view.delete.setOnClickListener { onDeleteClick(item) }
    }

    override fun onCreateView(context: Context): RecordPackItemView {
        return RecordPackItemView(context)
    }
}
