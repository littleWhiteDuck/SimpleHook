package me.simpleHook.ui.view.config

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import me.simpleHook.R
import me.simpleHook.util.dp

class CardText(context: Context) : MaterialCardView(context) {
    val tip = MaterialTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).also {
            it.setMargins(5.dp, 3.dp, 5.dp, 3.dp)
        }
        textAlignment = TEXT_ALIGNMENT_CENTER
        setBackgroundColor(Color.TRANSPARENT)
        addView(this)
    }

}