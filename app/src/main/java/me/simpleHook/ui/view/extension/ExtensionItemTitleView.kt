package me.simpleHook.ui.view.extension

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import me.simpleHook.extension.dp

class ExtensionItemTitleView(context: Context) : AppCompatTextView(context) {
    init {
        setPadding(0, 8.dp, 0, 8.dp)
        setTextColor("#4F9BFA".toColorInt())
        textSize = 4.5f.dp
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(16.dp, 0, 0, 0)
        }
    }
}