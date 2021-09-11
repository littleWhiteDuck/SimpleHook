package me.simpleHook.ui.view.main

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp
import kotlin.math.max

class AppConfigView(context: Context) : MaterialCardView(context) {
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

        val appName = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item))
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

        val switch = SwitchCompat(context).apply {
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
            switch.measure(switch.defaultWidthMeasureSpec(this), switch.defaultHeightMeasureSpec(this))
            val textViewWidth =
                measuredWidth - icon.measuredWidthWithMargins - switch.measuredWidthWithMarginsPaddings
            appName.measure(
                textViewWidth.toExactlyMeasureSpec(),
                appName.defaultHeightMeasureSpec(this)
            )
            desc.measure(textViewWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
            val height = max(icon.measuredHeightWithMargins, appName.measuredHeightWithMargins + desc.measuredHeightWithMargins)
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
            appName.autoLayout(icon.measuredHeightWithMargins + appName.marginLeft, appName.marginTop)
            desc.autoLayout(appName.left, appName.bottom + desc.marginTop)
            switch.autoLayout(switch.marginRight, switch.toVerticalCenter(this), fromRight = true)
        }
    }
}
