package me.simpleHook.ui.view.edit

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup

class InputView(context: Context) : CustomViewGroup(context) {


    val editText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    val textInputLayout = TextInputLayout(context).apply {
        setBoxCornerRadii(5f.dp, 5f.dp, 5f.dp, 5f.dp)
        boxBackgroundMode = BOX_BACKGROUND_OUTLINE
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    init {
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