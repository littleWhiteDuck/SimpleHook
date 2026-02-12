package me.simpleHook.feature.config.ui.view

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.core.extension.dp
import me.simpleHook.core.ui.custom.CustomViewGroup

class CollectionEnviItem(context: Context) : CustomViewGroup(context) {
    val editText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    val textInputLayout = TextInputLayout(context).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    init {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        textInputLayout.addView(editText)
        addView(textInputLayout)
        setPadding(16.dp, 5.dp, 16.dp, 0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        textInputLayout.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
            textInputLayout.defaultHeightMeasureSpec(this))
        setMeasuredDimension(measuredWidth,
            textInputLayout.measuredHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        textInputLayout.autoLayout(paddingStart, paddingTop)
    }
}