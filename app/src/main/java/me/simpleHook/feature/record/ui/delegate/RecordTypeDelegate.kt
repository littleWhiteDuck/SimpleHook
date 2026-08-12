package me.simpleHook.feature.record.ui.delegate

import android.content.Context
import android.view.ContextMenu
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.data.RecordShowType
import me.simpleHook.core.extension.dp
import me.simpleHook.feature.record.ui.view.RecordTypeItemView
import me.simpleHook.core.utils.IconHelper
import me.simpleHook.core.utils.RecordTypeUtils

class RecordTypeDelegate(
    private val onClick: (RecordShowType) -> Unit,
    private val onDeleteClick: (RecordShowType) -> Unit,
    private val onCreateContextMenu: (RecordShowType, ContextMenu) -> Unit
) : ViewDelegate<RecordShowType, RecordTypeItemView>() {
    override fun onBindView(view: RecordTypeItemView, item: RecordShowType) {
        view.container.apply {

            title.setText(item.type.displayId)
            tip.text = item.count.toString()

            val showText = RecordTypeUtils.getShowText(item.type)
            icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            setOnClickListener { onClick(item) }
            setOnCreateContextMenuListener { menu, _, _ ->
                onCreateContextMenu(item, menu)
            }
        }
        view.delete.setOnClickListener { onDeleteClick(item) }
    }

    override fun onCreateView(context: Context): RecordTypeItemView {
        return RecordTypeItemView(context)
    }

}
