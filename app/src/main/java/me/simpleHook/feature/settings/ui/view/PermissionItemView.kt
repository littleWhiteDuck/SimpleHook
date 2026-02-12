package me.simpleHook.feature.settings.ui.view


import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.google.android.material.card.MaterialCardView
import me.simpleHook.R
import me.simpleHook.core.ui.custom.CustomViewGroup
import me.simpleHook.core.extension.dp
import kotlin.math.max

class PermissionItemView(context: Context, attrs: AttributeSet?) :
    MaterialCardView(context, attrs) {
    constructor(context: Context) : this(context, null)

    val containerView = ContainerView(context).apply {
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

    class ContainerView(context: Context) : CustomViewGroup(context) {
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(40.dp, 40.dp).apply {
                setMargins(10.dp, 10.dp, 10.dp, 10.dp)
            }
            addView(this)
        }
        val appName =
            AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
                layoutParams = MarginLayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                ).also {
                    it.setMargins(10.dp, 8.dp, 0, 0)
                }
                addView(this)
            }
        val packageName = AppCompatTextView(
            ContextThemeWrapper(
                context, R.style.text_view_item_secondary
            )
        ).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 5.dp, 0, 8.dp)
                }
            addView(this)
        }

        val checkBox = CheckBox(context).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(5.dp, 5.dp, 5.dp, 5.dp)
                }
            isClickable = false
            setBackgroundColor(Color.TRANSPARENT)
            addView(this)
        }


        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            checkBox.autoMeasure()
            val textViewWidth =
                measuredWidth - icon.measuredWidthWithMargins - checkBox.measuredWidthWithMargins
            appName.measure(
                textViewWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this)
            )
            packageName.measure(
                textViewWidth.toExactlyMeasureSpec(), packageName.defaultHeightMeasureSpec(this)
            )
            val height = max(
                icon.measuredHeightWithMargins,
                appName.measuredHeight + packageName.measuredHeightWithMargins
            )
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {

            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
            appName.autoLayout(icon.measuredWidthWithMargins, appName.marginTop)
            packageName.autoLayout(appName.left, appName.bottom + packageName.marginTop)
            checkBox.autoLayout(
                checkBox.marginEnd, checkBox.toVerticalCenter(this), fromRight = true
            )

        }
    }
}


