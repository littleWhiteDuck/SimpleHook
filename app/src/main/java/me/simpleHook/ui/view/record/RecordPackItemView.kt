package me.simpleHook.ui.view.record

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomSwipeCardViewGroup
import me.simpleHook.ui.custom.CustomViewGroup
import kotlin.math.max


class RecordPackItemView(context: Context) :
    CustomSwipeCardViewGroup(ContextThemeWrapper(context, R.style.card)) {
    val container = RecordContainerView(context)
    val delete = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT).also {
            setPadding(0, 0, 0, 0)
        }
        text = context.getString(R.string.delete)
        setTextColor(Color.WHITE)
        setBackgroundColor("#FF80AB".toColorInt())
        addView(this)
    }

    init {
        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1.dp.toFloat()
        radius = 5.dp.toFloat()
        addView(container)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        container.autoMeasure()
        delete.measure(delete.defaultWidthMeasureSpec(this),
            container.measuredHeight.toExactlyMeasureSpec())
        mRightMenuWidths = delete.measuredWidth
        mContentView = container
        setMeasuredDimension(measuredWidth, container.measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        container.autoLayout(x = 0, y = 0)
        delete.autoLayout(x = container.right, y = 0)
    }

    class RecordContainerView(context: Context) : CustomViewGroup(context) {
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(40.dp, 40.dp).apply {
                setMargins(10.dp, 10.dp, 10.dp, 10.dp)
            }
            addView(this)
        }

        val title = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 8.dp, 0, 0)
                }
            addView(this)
        }

        val desc = AppCompatTextView(ContextThemeWrapper(context,
            R.style.text_view_item_secondary)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 5.dp, 0, 8.dp)
                }
            addView(this)
        }

        val tip = AppCompatTextView(ContextThemeWrapper(context,
            R.style.text_view_item_secondary)).apply {
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
            tip.autoMeasure()
            val textViewWidth =
                measuredWidth - icon.measuredWidthWithMargins - tip.measuredWidthWithMargins
            title.measure(textViewWidth.toExactlyMeasureSpec(),
                title.defaultHeightMeasureSpec(this))
            desc.measure(textViewWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
            val height = max(icon.measuredHeightWithMargins,
                title.measuredHeightWithMargins + desc.measuredHeightWithMargins)
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
            tip.autoLayout(tip.marginRight, tip.toVerticalCenter(this), fromRight = true)
            title.autoLayout(icon.measuredHeightWithMargins + title.marginLeft, title.marginTop)
            desc.autoLayout(title.left, title.bottom + desc.marginTop)
        }
    }
}