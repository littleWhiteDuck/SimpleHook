package me.simpleHook.ui.view.extension

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.extension.dp
import me.simpleHook.extension.getColorByAttr

@SuppressLint("ResourceType", "UseCompatLoadingForColorStateLists")
class ExtensionItemTitleView(context: Context) : AppCompatTextView(context) {
    init {
        setPadding(0, 8.dp, 0, 8.dp)
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorPrimary))
        textSize = 4.5f.dp
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(16.dp, 0, 0, 0)
        }
    }
}