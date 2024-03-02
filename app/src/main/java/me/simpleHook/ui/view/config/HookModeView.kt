package me.simpleHook.ui.view.config

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.card.MaterialCardView
import me.simpleHook.extension.dp
import me.simpleHook.extension.getColorByAttr

class HookModeView(context: Context) : MaterialCardView(context) {
    val title = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
    }

    init {
        addView(title)
        setContentPadding(10.dp, 10.dp, 10.dp, 10.dp)
        strokeWidth = 0
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorPrimary)
        layoutParams = MarginLayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 3f.dp
        radius = 5f.dp
    }

}