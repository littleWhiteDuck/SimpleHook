package me.simpleHook.ui.view.edit

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp

class InputView(context: Context) : CustomViewGroup(context) {


    val editText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val textInputLayout = TextInputLayout(context).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    init {
        textInputLayout.addView(editText)
        addView(textInputLayout)
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        textInputLayout.autoMeasure()
        setMeasuredDimension(
            measuredWidth,
            textInputLayout.measuredHeight + paddingTop + paddingBottom
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        textInputLayout.autoLayout(paddingStart, paddingTop)
    }

}