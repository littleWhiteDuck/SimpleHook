package me.simpleHook.ui.view.config

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.google.android.material.textview.MaterialTextView
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp


class ConfigItemView(context: Context) : CustomViewGroup(context) {

    init {
        val typedValue = TypedValue()
        getContext().theme
            .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
    }

    val num = MaterialTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
        addView(this)
    }
    val className = MaterialTextView(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
            it.marginStart = 5.dp
        }
        addView(this)
    }

    val otherName = MaterialTextView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(0, 5.dp, 0, 0)
            }
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        num.measure(num.defaultWidthMeasureSpec(this), num.defaultHeightMeasureSpec(this))
        val leftWidth = measuredWidth - paddingStart - paddingEnd - num.measuredWidthWithMarginsPaddings - className.marginStart
        className.measure(
            leftWidth.toExactlyMeasureSpec(),
            className.defaultHeightMeasureSpec(this)
        )
        otherName.measure(
            leftWidth.toExactlyMeasureSpec(),
            otherName.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            className.measuredHeight + otherName.measuredHeightWithMargins + paddingTop + paddingBottom
        )
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        num.autoLayout(paddingStart + num.paddingStart, num.toVerticalCenter(this))
        className.autoLayout(num.right + num.paddingEnd + className.marginStart, paddingTop)
        otherName.autoLayout(className.left, className.bottom + otherName.marginTop)
    }
}