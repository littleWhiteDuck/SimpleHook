package me.simpleHook.ui.view.backup

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup
import kotlin.math.max

class RestoreItemView(context: Context) : CustomViewGroup(context) {
    val icon = AppCompatImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(16.dp, 0, 10.dp, 0)
        addView(this)
    }

    @Suppress("DEPRECATION")
    val desc = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(0, 0, 16.dp, 0)
        setTextColor(context.resources.getColor(R.color.normal_text_color))
        addView(this)
    }

    val time = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val typedValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground,
            typedValue,
            true)
        val attribute = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        setPadding(0, 5.dp, 0, 5.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        icon.autoMeasure()
        val leftWidth = measuredWidth - icon.measuredWidth
        desc.measure(leftWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        time.measure(leftWidth.toExactlyMeasureSpec(), time.defaultHeightMeasureSpec(this))
        val height = max(icon.measuredHeight,
            desc.measuredHeight + time.measuredHeight) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        icon.autoLayout(0, paddingTop)
        desc.autoLayout(icon.right)
        time.autoLayout(icon.right, desc.bottom)
    }
}