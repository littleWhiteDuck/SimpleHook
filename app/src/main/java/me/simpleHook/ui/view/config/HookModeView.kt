package me.simpleHook.ui.view.config

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import com.google.android.material.card.MaterialCardView
import me.simpleHook.extension.dp

class HookModeView(context: Context) : MaterialCardView(context) {
    val title = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
    }

    init {
        strokeColor = "#4F9BFA".toColorInt()
        strokeWidth = 1.5f.dp.toInt()
        setContentPadding(10.dp, 10.dp, 10.dp, 10.dp)
        addView(title)
        layoutParams = MarginLayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        radius = 5f.dp
    }

}