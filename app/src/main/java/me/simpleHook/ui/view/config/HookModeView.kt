package me.simpleHook.ui.view.config

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
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
        setContentPadding(12.dp, 12.dp, 12.dp, 12.dp)
        addView(title)
        layoutParams = MarginLayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1f.dp
        radius = 5f.dp
    }

}