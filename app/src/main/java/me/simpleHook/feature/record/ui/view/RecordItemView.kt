package me.simpleHook.feature.record.ui.view

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.ui.custom.CustomSwipeCardViewGroup
import me.simpleHook.core.ui.custom.CustomViewGroup
import kotlin.math.max


class RecordItemView(context: Context) :
    CustomSwipeCardViewGroup(ContextThemeWrapper(context, R.style.card)) {
    val container = RecordContainerView(context)
    val mark = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        ).also {
            setPadding(0, 0, 0, 0)
        }
        text = context.getString(R.string.mark)
        setTextColor(Color.WHITE)
        setBackgroundColor("#4F9BFA".toColorInt())
        addView(this)
    }
    val delete = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
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
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1f.dp
        radius = 5f.dp
        strokeWidth = 0
        addView(container)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        container.autoMeasure()
        mark.measure(
            mark.defaultWidthMeasureSpec(this),
            container.measuredHeight.toExactlyMeasureSpec()
        )
        delete.autoMeasure()
        isClickable = true
        mRightMenuWidths = mark.measuredWidth + delete.measuredWidth
        mContentView = container
        setMeasuredDimension(measuredWidth, container.measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        container.autoLayout(x = 0, y = 0)
        mark.autoLayout(x = container.right, y = 0)
        delete.autoLayout(x = mark.right, y = 0)
    }

    class RecordContainerView(context: Context) : CustomViewGroup(context) {
        val icon = AppCompatImageView(context).apply {
            layoutParams = MarginLayoutParams(40.dp, 40.dp).apply {
                setMargins(10.dp, 12.dp, 10.dp, 10.dp)
            }
            addView(this)
        }

        val title = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 8.dp, 0, 0)
                }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            addView(this)
        }

        val signature = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item_secondary)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 2.dp, 0, 0)
                }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
            addView(this)
        }

        val meta = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 4.dp, 0, 8.dp)
                }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            addView(this)
        }

        val time = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(10.dp, 4.dp, 0, 8.dp)
                }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            addView(this)
        }

        val tip = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item_secondary)).apply {
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
            val textLeft = icon.measuredWidthWithMargins + title.marginLeft
            val textRight = measuredWidth - tip.measuredWidthWithMargins
            val textViewWidth = (textRight - textLeft).coerceAtLeast(0)
            title.measure(textViewWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
            signature.measureIfVisible(textViewWidth)
            val bottomTextWidth = if (meta.isVisible()) textViewWidth / 2 else 0
            meta.measureIfVisible(bottomTextWidth)
            time.measure((textViewWidth - bottomTextWidth).toExactlyMeasureSpec(), time.defaultHeightMeasureSpec(this))
            val textHeight = title.measuredHeightWithMargins +
                    signature.visibleHeightWithMargins() +
                    max(meta.visibleHeightWithMargins(), time.measuredHeightWithMargins)
            val height = max(icon.measuredHeightWithMargins, textHeight)
            setMeasuredDimension(measuredWidth, height)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
            tip.autoLayout(tip.marginRight, tip.toVerticalCenter(this), fromRight = true)
            val textLeft = icon.measuredWidthWithMargins + title.marginLeft
            title.autoLayout(textLeft, title.marginTop)
            val bottomAnchor = if (signature.isVisible()) {
                signature.autoLayout(textLeft, title.bottom + signature.marginTop)
                signature.bottom
            } else {
                title.bottom
            }
            if (meta.isVisible()) {
                meta.autoLayout(textLeft, bottomAnchor + meta.marginTop)
            }
            time.autoLayout(
                tip.measuredWidthWithMargins + time.marginRight,
                bottomAnchor + time.marginTop,
                fromRight = true
            )
        }

        private fun View.measureIfVisible(width: Int) {
            if (isVisible()) {
                measure(width.toExactlyMeasureSpec(), defaultHeightMeasureSpec(this@RecordContainerView))
            } else {
                measure(0.toExactlyMeasureSpec(), 0.toExactlyMeasureSpec())
            }
        }

        private fun View.visibleHeightWithMargins(): Int {
            return if (isVisible()) measuredHeightWithMargins else 0
        }

        private fun View.isVisible(): Boolean {
            return visibility != View.GONE
        }
    }
}
