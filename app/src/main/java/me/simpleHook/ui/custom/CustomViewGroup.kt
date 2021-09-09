package me.simpleHook.ui.custom

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop

abstract class CustomViewGroup(context: Context) : ViewGroup(context) {

    protected val View.measuredWidthWithMargins get() = measuredWidth + marginStart + marginEnd
    protected val View.measuredHeightWithMargins get() = measuredHeight + marginTop + marginBottom

    protected val View.measuredWidthWithMarginsPaddings
        get() = measuredWidth + marginStart + marginEnd + paddingStart + paddingEnd
    protected val View.measuredHeightWithMarginsPaddings
        get() = measuredHeight + marginTop + marginBottom + paddingTop + paddingBottom

    protected fun Int.toExactlyMeasureSpec() =
        MeasureSpec.makeMeasureSpec(this, MeasureSpec.EXACTLY)

    protected fun Int.toAtMostMeasureSpec() = MeasureSpec.makeMeasureSpec(this, MeasureSpec.AT_MOST)

    protected fun View.defaultWidthMeasureSpec(parent: ViewGroup): Int {
        return when (layoutParams.width) {
            MATCH_PARENT -> parent.measuredWidth.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("error because error")
            else -> layoutParams.width.toExactlyMeasureSpec()
        }
    }

    protected fun View.defaultHeightMeasureSpec(parent: ViewGroup): Int {
        return when (layoutParams.height) {
            MATCH_PARENT -> parent.measuredHeight.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("error because error")
            else -> layoutParams.height.toExactlyMeasureSpec()
        }
    }

    protected fun View.autoMeasure() {
        measure(
            this.defaultWidthMeasureSpec(this@CustomViewGroup),
            this.defaultHeightMeasureSpec(this@CustomViewGroup)
        )
    }

    protected fun View.autoLayout(x: Int = 0, y: Int = 0, fromRight: Boolean = false) {
        if (fromRight) {
            autoLayout(this@CustomViewGroup.measuredWidth - x - measuredWidth, y)
        } else {
            layout(x, y, x + measuredWidth, y + measuredHeight)
        }
    }

    protected fun View.toVerticalCenter(parent: ViewGroup): Int {
        return (parent.measuredHeight - measuredHeight) / 2
    }
}
