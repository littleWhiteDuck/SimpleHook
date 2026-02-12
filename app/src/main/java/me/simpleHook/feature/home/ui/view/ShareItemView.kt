package me.simpleHook.feature.home.ui.view

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.R
import me.simpleHook.core.ui.custom.CustomViewGroup
import me.simpleHook.core.extension.dp
import kotlin.math.max

class ShareItemView(context: Context) : CustomViewGroup(context) {
    init {
        val typedValue = TypedValue()
        getContext().theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        setPadding(10.dp, 2.dp, 2.dp, 5.dp)
    }

    val information = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    val checkBox = AppCompatCheckBox(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        isClickable = false
        setBackgroundColor(Color.TRANSPARENT)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkBox.autoMeasure()
        information.measure(
            measuredWidth - paddingStart - paddingEnd - checkBox.measuredWidth,
            information.defaultHeightMeasureSpec(this)
        )
        val height =
            max(checkBox.measuredHeight, information.measuredHeight) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        information.autoLayout(paddingStart, information.toVerticalCenter(this))
        checkBox.autoLayout(paddingEnd, checkBox.toVerticalCenter(this), fromRight = true)
    }
}