package me.simpleHook.ui.view.applist

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp
import kotlin.math.max

class AppItemView(context: Context, attrs: AttributeSet?) : MaterialCardView(context, attrs) {
    constructor(context: Context) : this(context, null)

    val containerView = AppListItem(context).apply {
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
    }

    init {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
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
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                addView(this)
            }
        val packageName = AppCompatTextView(
            ContextThemeWrapper(
                context, R.style.text_view_item_secondary
            )
        ).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 5.dp, 0, 0)
                }
            addView(this)
        }

        val otherInfo = AppCompatTextView(
            ContextThemeWrapper(
                context, R.style.text_view_item_secondary
            )
        ).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 5.dp, 0, 0)
                }
            addView(this)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            val textViewWidth =
                measuredWidth - icon.measuredWidthWithMargins - paddingStart - paddingEnd
            appName.measure(
                textViewWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this)
            )
            packageName.measure(
                textViewWidth.toExactlyMeasureSpec(), packageName.defaultHeightMeasureSpec(this)
            )
            otherInfo.measure(
                textViewWidth.toExactlyMeasureSpec(), otherInfo.defaultHeightMeasureSpec(this)
            )
            val height = max(
                icon.marginTop + icon.measuredHeight + icon.marginBottom,
                appName.measuredHeight + packageName.measuredHeightWithMargins + otherInfo.measuredHeightWithMargins
            )
            setMeasuredDimension(measuredWidth, height + paddingTop + paddingEnd)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {

            icon.autoLayout(icon.marginLeft + paddingStart, icon.toVerticalCenter(this))
            if (packageName.text.toString().isNotEmpty()) {
                appName.autoLayout(
                    icon.measuredWidthWithMargins + paddingStart, appName.marginTop + paddingTop
                )
                packageName.autoLayout(appName.left, appName.bottom + packageName.marginTop)
                otherInfo.autoLayout(appName.left, packageName.bottom + otherInfo.marginTop)
            } else {
                appName.autoLayout(
                    icon.measuredWidthWithMargins + paddingStart, appName.toVerticalCenter(this)
                )
            }

        }
    }
}
