package me.simpleHook.recyclerview.delegate

import android.content.Context
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.bean.RecordShowPack
import me.simpleHook.ui.view.record.RecordPackItemView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.GlideApp
import me.simpleHook.util.IconHelper

class RecordPackDelegate(
    val onClick: (RecordShowPack) -> Unit, val onDeleteClick: (RecordShowPack) -> Unit
) : ViewDelegate<RecordShowPack, RecordPackItemView>() {
    override fun onBindView(view: RecordPackItemView, item: RecordShowPack) {
        view.container.apply {
            if (item.packageName.startsWith("error.")) {
                title.text = "Hook Error"
                icon.setImageDrawable(IconHelper.getTextIcon(text = "Error"))
            } else {
                GlideApp.with(icon).load(item.packageName).into(icon)
                title.text = AppUtils.getAppName(view.context, item.packageName)
            }

            desc.text = item.packageName
            tip.text = item.count.toString()
            setOnClickListener { onClick(item) }
        }
        view.delete.setOnClickListener { onDeleteClick(item) }
    }

    override fun onCreateView(context: Context): RecordPackItemView {
        return RecordPackItemView(context)
    }
}