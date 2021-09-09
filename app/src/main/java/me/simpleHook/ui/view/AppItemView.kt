package me.simpleHook.ui.view

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp
import kotlin.math.max

class AppItemView(context: Context) : MaterialCardView(context) {
    val containerView = AppListItem(context).apply {
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
    }

    init {
        addView(containerView)
    }

    class AppListItem(context: Context) : CustomViewGroup(context) {
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(50.dp, 50.dp).apply {
                setMargins(10.dp, 10.dp, 10.dp, 10.dp)
            }
            addView(this)
        }
        val appName =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_subtitle1)).apply {
                layoutParams =
                    MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 5.dp, 10.dp, 0)
                    }
                addView(this)
            }
        val packageName = AppCompatTextView(
            ContextThemeWrapper(
                context,
                R.style.text_view_item_secondary
            )
        ).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 5.dp, 10.dp, 0)
                }
            addView(this)
        }

        val otherInfo = AppCompatTextView(
            ContextThemeWrapper(
                context,
                R.style.text_view_item_secondary
            )
        ).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 5.dp, 10.dp, 5.dp)
                }
            addView(this)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            val textViewWidth =
                measuredWidth - icon.paddingLeft - icon.measuredWidth - icon.paddingRight
            appName.measure(
                textViewWidth.toExactlyMeasureSpec(),
                appName.defaultHeightMeasureSpec(this)
            )
            packageName.measure(
                textViewWidth.toExactlyMeasureSpec(),
                packageName.defaultHeightMeasureSpec(this)
            )
            otherInfo.measure(
                textViewWidth.toExactlyMeasureSpec(),
                otherInfo.defaultHeightMeasureSpec(this)
            )
            val height = max(
                icon.marginTop + icon.measuredHeight + icon.marginBottom,
                appName.marginTop + appName.measuredHeight + packageName.marginTop + packageName.measuredHeight +
                        otherInfo.marginTop + otherInfo.measuredHeight + otherInfo.marginBottom
            )
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {

            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))

            appName.autoLayout(
                icon.marginLeft + icon.measuredWidth + icon.marginRight,
                appName.marginTop
            )

            packageName.autoLayout(appName.left, appName.bottom + packageName.marginTop)

            otherInfo.autoLayout(appName.left, packageName.bottom + otherInfo.marginTop)
        }
    }
}
