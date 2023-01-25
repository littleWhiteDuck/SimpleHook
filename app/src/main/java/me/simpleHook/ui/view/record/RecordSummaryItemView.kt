package me.simpleHook.ui.view.record

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColorInt
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomSwipeCardViewGroup
import me.simpleHook.util.dp


class RecordSummaryItemView(context: Context) :
    CustomSwipeCardViewGroup(ContextThemeWrapper(context, R.style.card)) {
    val container = RecordContainerView(context)
    val delete = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).also {
            setPadding(0, 0, 0, 0)
        }
        text = context.getString(R.string.delete)
        setTextColor(Color.WHITE)
        setBackgroundColor("#FF80AB".toColorInt())
        addView(this)
    }

    init {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1.dp.toFloat()
        radius = 5.dp.toFloat()
        addView(container)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        container.autoMeasure()
        delete.measure(
            delete.defaultWidthMeasureSpec(this), container.measuredHeight.toExactlyMeasureSpec()
        )
        isClickable = true
        mRightMenuWidths = delete.measuredWidth
        mContentView = container
        setMeasuredDimension(measuredWidth, container.measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        container.autoLayout(x = 0, y = 0)
        delete.autoLayout(x = container.right, y = 0)
    }
}