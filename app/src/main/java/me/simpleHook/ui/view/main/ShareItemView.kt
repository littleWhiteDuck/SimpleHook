package me.simpleHook.ui.view.main

import android.content.Context
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp
import kotlin.math.max

class ShareItemView(context: Context) : CustomViewGroup(context) {
    init {
        setPadding(10.dp, 2.dp, 2.dp ,5.dp)
    }
    val information = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    val checkBox = AppCompatCheckBox(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkBox.autoMeasure()
        information.measure(measuredWidth - paddingStart - paddingEnd - checkBox.measuredWidth, information.defaultHeightMeasureSpec(this))
        val height = max(checkBox.measuredHeight, information.measuredHeight) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        information.autoLayout(paddingStart, information.toVerticalCenter(this))
        checkBox.autoLayout(paddingEnd, checkBox.toVerticalCenter(this), fromRight = true)
    }
}