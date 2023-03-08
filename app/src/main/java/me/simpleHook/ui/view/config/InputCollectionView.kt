package me.simpleHook.ui.view.config

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup

class InputCollectionView(context: Context) : CustomViewGroup(context) {
    val nameEditText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    val nameInput = TextInputLayout(context).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        helperText = "给收藏起个名字"
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    val configEditText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    val configInput = TextInputLayout(context).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        helperText = "编辑配置"
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    init {
        nameInput.addView(nameEditText)
        configInput.addView(configEditText)
        addView(nameInput)
        addView(configInput)
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val leftWidth = measuredWidth - paddingStart - paddingEnd
        nameInput.measure(leftWidth.toExactlyMeasureSpec(),
            nameInput.defaultHeightMeasureSpec(this))
        configInput.measure(leftWidth.toExactlyMeasureSpec(),
            nameInput.defaultHeightMeasureSpec(this))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        nameInput.autoLayout(paddingStart, paddingTop)
        configInput.autoLayout(paddingStart, nameInput.bottom)
    }
}