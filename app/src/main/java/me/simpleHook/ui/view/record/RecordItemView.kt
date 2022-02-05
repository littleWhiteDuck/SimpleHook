package me.simpleHook.ui.view.record

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp
import kotlin.math.max


class RecordItemView(context: Context) : MaterialCardView(context) {
    val container = ContainerView(context)

    init {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1.dp.toFloat()
        radius = 5.dp.toFloat()
        addView(container)
    }

    class ContainerView(context: Context) : CustomViewGroup(context) {
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(40.dp, 40.dp).apply {
                setMargins(10.dp, 10.dp, 10.dp, 10.dp)
            }
            addView(this)
        }

        val title = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item))
            .apply {
                layoutParams =
                    MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                        it.setMargins(10.dp, 8.dp, 0, 0)
                    }
                addView(this)
            }

        val desc = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item_secondary))
            .apply {
                layoutParams =
                    MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                        it.setMargins(10.dp, 5.dp, 0, 8.dp)
                    }
                addView(this)
            }

        val tip =
            AppCompatTextView(
                ContextThemeWrapper(
                    context,
                    R.style.text_view_item_secondary
                )
            ).apply {
                layoutParams =
                    MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                        it.setMargins(0.dp, 0.dp, 5.dp, 0.dp)
                    }
                setPadding(5.dp, 5.dp, 5.dp, 5.dp)
                addView(this)
            }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            tip.measure(
                tip.defaultWidthMeasureSpec(this),
                tip.defaultHeightMeasureSpec(this)
            )
            val textViewWidth =
                measuredWidth - icon.measuredWidthWithMargins - tip.measuredWidthWithMarginsPaddings
            title.measure(
                textViewWidth.toExactlyMeasureSpec(),
                title.defaultHeightMeasureSpec(this)
            )
            if (desc.visibility != View.GONE) desc.measure(
                textViewWidth.toExactlyMeasureSpec(),
                desc.defaultHeightMeasureSpec(this)
            )
            val height = max(
                icon.measuredHeightWithMargins,
                title.measuredHeightWithMargins + desc.measuredHeightWithMargins
            )
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
            tip.autoLayout(
                tip.marginRight,
                tip.toVerticalCenter(this),
                fromRight = true
            )
            if (desc.visibility != View.GONE) {
                title.autoLayout(icon.measuredHeightWithMargins + title.marginLeft, title.marginTop)
                desc.autoLayout(title.left, title.bottom + desc.marginTop)
            } else {
                title.autoLayout(
                    icon.measuredHeightWithMargins + title.marginLeft,
                    title.toVerticalCenter(this)
                )
            }
        }
    }
}