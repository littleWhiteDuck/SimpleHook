package me.simpleHook.ui.view.extension

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup
import kotlin.math.max

class SubNextItemView(context: Context) : CustomViewGroup(context) {
    init {
        setBackgroundResource(R.drawable.extension_item_card_bg)
        setPadding(16.dp, 5.dp, 5.dp, 5.dp)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    val desc =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item_secondary)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(0, 2.dp, 0, 0)
                }
            addView(this)
        }

    private val nextIcon = AppCompatImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setImageResource(R.drawable.ic_next_page)
        setPadding(5.dp, 10.dp, 15.dp, 10.dp)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val leftWidth = measuredWidth - paddingStart - paddingEnd - nextIcon.measuredWidth
        nextIcon.autoMeasure()
        title.measure(leftWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
        desc.measure(leftWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        val height = max(
            title.measuredHeight + desc.measuredHeightWithMargins,
            nextIcon.measuredHeight
        ) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        title.autoLayout(paddingStart + title.marginLeft, paddingTop)
        desc.autoLayout(title.left, title.bottom + desc.marginTop)
        nextIcon.autoLayout(paddingEnd, nextIcon.toVerticalCenter(this), fromRight = true)
    }
}