package me.simpleHook.ui.view.about

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp

class OpenSourceView(context: Context) : CustomViewGroup(context) {
    init {
        val typedValue = TypedValue()
        getContext().theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        setPadding(12.dp, 12.dp, 12.dp, 12.dp)
    }

    val name = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_subtitle2)).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    val openSource =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(0, 3.dp, 0, 0)
                }
            addView(this)
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val textViewWidth = measuredWidth - paddingLeft - paddingRight
        name.measure(textViewWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
        openSource.measure(
            textViewWidth.toExactlyMeasureSpec(),
            name.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            paddingTop + name.measuredHeight + openSource.measuredHeight + openSource.marginTop + paddingBottom
        )
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        name.autoLayout(paddingLeft, paddingTop)
        openSource.autoLayout(paddingLeft, name.bottom + openSource.marginTop)
    }
}