package me.simpleHook.recyclerview.delegate

import android.content.Context
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.data.RecordShowType
import me.simpleHook.extension.dp
import me.simpleHook.ui.view.record.RecordTypeItemView
import me.simpleHook.util.IconHelper

class RecordTypeDelegate(
    val onClick: (RecordShowType) -> Unit, val onDeleteClick: (RecordShowType) -> Unit
) : ViewDelegate<RecordShowType, RecordTypeItemView>() {
    override fun onBindView(view: RecordTypeItemView, item: RecordShowType) {
        view.container.apply {
            title.text = if (item.type.startsWith("Error")) "Hook Error" else item.type
            tip.text = item.count.toString()
            val showText = me.simpleHook.util.RecordType.getShowText(item.type)
            icon.setImageDrawable(IconHelper.getTextIcon(40f.dp, showText))
            setOnClickListener { onClick(item) }
        }
        view.delete.setOnClickListener { onDeleteClick(item) }
    }

    override fun onCreateView(context: Context): RecordTypeItemView {
        return RecordTypeItemView(context)
    }

}