package me.simpleHook.ui.view.config

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp


class ConfigItemView(context: Context) : MaterialCardView(context) {
    val containerView = ConfigContainerView(context).apply {
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
    }

    init {
        addView(containerView)
    }

    class ConfigContainerView(context: Context) : CustomViewGroup(context) {
        val num =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_sans_serif)).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                setPadding(5.dp, 5.dp, 5.dp, 5.dp)
                addView(this)
            }
        val className =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
                layoutParams = MarginLayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.marginStart = 5.dp
                }
                addView(this)
            }

        val otherName = MaterialTextView(context).apply {
            layoutParams = MarginLayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 5.dp, 0, 0)
            }
            addView(this)
        }
        val tip = AppCompatTextView(context).apply {
            layoutParams = MarginLayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 5.dp, 0, 0)
            }
            addView(this)
        }

        val enable = CheckBox(context).apply {
            layoutParams = MarginLayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart = 3.dp
                it.marginEnd = 3.dp
                it.topMargin = 3.dp
                it.bottomMargin = 3.dp
            }
            addView(this)
        }


        init {
            val typedValue = TypedValue()
            getContext().theme.resolveAttribute(
                android.R.attr.selectableItemBackground, typedValue, true
            )
            val attribute = intArrayOf(android.R.attr.selectableItemBackground)
            val typedArray =
                getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
            background = typedArray.getDrawable(0)
            setPadding(5.dp, 5.dp, 5.dp, 5.dp)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            enable.autoMeasure()
            num.measure(num.defaultWidthMeasureSpec(this), num.defaultHeightMeasureSpec(this))
            tip.autoMeasure()
            val leftWidth =
                measuredWidth - paddingStart - paddingEnd - num.measuredWidthWithMargins - className.marginStart - enable.measuredWidthWithMargins
            className.measure(
                leftWidth.toExactlyMeasureSpec(), className.defaultHeightMeasureSpec(this)
            )
            otherName.measure(
                leftWidth.toExactlyMeasureSpec(), otherName.defaultHeightMeasureSpec(this)
            )
            setMeasuredDimension(
                measuredWidth,
                tip.measuredHeightWithMargins + className.measuredHeight + otherName.measuredHeightWithMargins + paddingTop + paddingBottom
            )
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
            enable.autoLayout(paddingEnd + enable.marginEnd, enable.toVerticalCenter(this), true)
            num.autoLayout(paddingStart + num.paddingStart, num.toVerticalCenter(this))
            className.autoLayout(num.right + num.paddingEnd + className.marginStart, paddingTop)
            otherName.autoLayout(className.left, className.bottom + otherName.marginTop)
            tip.autoLayout(className.left, otherName.bottom + tip.marginTop)
        }
    }
}