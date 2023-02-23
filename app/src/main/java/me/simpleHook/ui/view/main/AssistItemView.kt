package me.simpleHook.ui.view.main

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp

class AssistItemView(context: Context) : MaterialCardView(context) {
    val containerView = ContainerView(context)

    init {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 5.dp)
        }
        cardElevation = 1.dp.toFloat()
        radius = 5.dp.toFloat()
        addView(containerView)
    }

    class ContainerView(context: Context) : CustomViewGroup(context) {

        val appName =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
        val versionName =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
                layoutParams =
                    MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                        it.setMargins(0, 8.dp, 8.dp, 0)
                    }
            }
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(50.dp, 50.dp).also {
                it.setMargins(0, 0, 0, 8.dp)
            }
        }

        init {
            setPadding(5.dp, 5.dp, 5.dp, 5.dp)
            addView(appName)
            addView(versionName)
            addView(icon)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            appName.measure(
                (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
                appName.defaultHeightMeasureSpec(this)
            )
            icon.autoMeasure()
            val remainWidth =
                measuredWidth - paddingStart - paddingEnd - versionName.marginEnd - icon.measuredWidth
            versionName.measure(
                remainWidth.toExactlyMeasureSpec(),
                versionName.defaultHeightMeasureSpec(this)
            )
            setMeasuredDimension(
                measuredWidth,
                paddingTop + appName.measuredHeight + icon.measuredHeightWithMargins + paddingBottom
            )
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
            appName.autoLayout(paddingStart, paddingTop)
            versionName.autoLayout(paddingStart, appName.bottom + versionName.marginTop)
            icon.autoLayout(
                paddingEnd,
                measuredHeight - icon.measuredHeightWithMargins - paddingBottom,
                fromRight = true
            )
        }
    }
}
