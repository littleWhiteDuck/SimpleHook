package me.simpleHook.ui.view.assist


import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp


class AssistItemView(context: Context) : CustomViewGroup(context) {
    init {
        val typedValue = TypedValue()
        getContext().theme
            .resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        setPadding(16.dp, 5.dp, 16.dp, 5.dp)
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
    val control = AppCompatTextView(context).apply {
        layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        control.autoMeasure()
        val textViewWidth = measuredWidth - control.measuredWidthWithMargins - paddingStart - paddingEnd
        title.measure(textViewWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
        desc.measure(textViewWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        val height = if (desc.visibility == GONE) paddingTop + title.measuredHeight + paddingBottom else paddingTop + title.measuredHeight + desc.measuredHeightWithMargins + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        title.autoLayout(paddingStart + title.marginLeft, paddingTop)
        desc.autoLayout(title.left, title.bottom + desc.marginTop)
        control.autoLayout(paddingEnd + control.marginRight, control.toVerticalCenter(this), fromRight = true)
    }
}