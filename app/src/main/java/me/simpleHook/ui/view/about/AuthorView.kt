package me.simpleHook.ui.view.about

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp

class AuthorView(context: Context) : CustomViewGroup(context) {
    init {
        val typedValue = TypedValue()
        getContext().theme
            .resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        isFocusable = true
        isClickable = true
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    }
    val icon = AppCompatImageView(context).apply {
        layoutParams = MarginLayoutParams(50.dp, 50.dp).also {
            it.setMargins(8.dp, 0, 0, 0)
        }
        addView(this)
    }
    val name = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_subtitle1)).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
            it.setMargins(8.dp, 0, 0, 0)
        }
        addView(this)
    }

    val introduce = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
            it.setMargins(8.dp, 8.dp, 0, 0)
        }
        addView(this)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        icon.autoMeasure()
        val textViewWidth = measuredWidth - paddingLeft - paddingRight - icon.measuredWidthWithMargins
        name.measure(textViewWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
        introduce.measure(textViewWidth.toExactlyMeasureSpec(), introduce.defaultHeightMeasureSpec(this))
        setMeasuredDimension(measuredWidth, paddingTop + name.measuredHeight + introduce.measuredHeightWithMargins + paddingBottom)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        icon.autoLayout(paddingLeft + icon.marginLeft, paddingTop)
        name.autoLayout(icon.right + name.marginStart, paddingTop)
        introduce.autoLayout(name.left, name.bottom + introduce.marginTop)
    }
}