package me.simpleHook.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.google.android.material.textview.MaterialTextView
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp

@SuppressLint("ResourceType")
class ConfigView(context: Context) : CustomViewGroup(context) {

    init {
        val typedValue = TypedValue()
        getContext().theme
            .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
    }

    val num = MaterialTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
        gravity = Gravity.CENTER
        addView(this)
    }
    val className = MaterialTextView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(5.dp, 5.dp, 5.dp, 0.dp)
            }
        addView(this)
    }

    val otherName = MaterialTextView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(5.dp, 5.dp, 5.dp, 5.dp)
            }
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        num.measure(
            (measuredWidth * 0.15).toInt().toExactlyMeasureSpec(),
            num.defaultHeightMeasureSpec(this)
        )
        className.measure(
            (measuredWidth * 0.85).toInt().toExactlyMeasureSpec(),
            className.defaultHeightMeasureSpec(this)
        )
        otherName.measure(
            (measuredWidth * 0.85).toInt().toExactlyMeasureSpec(),
            otherName.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            className.measuredHeightWithMargins + otherName.measuredHeightWithMargins
        )
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        num.autoLayout(num.marginLeft, num.toVerticalCenter(this))
        className.autoLayout(num.right + className.marginLeft, className.marginTop)
        otherName.autoLayout(className.left, className.bottom + otherName.marginTop)
    }
}