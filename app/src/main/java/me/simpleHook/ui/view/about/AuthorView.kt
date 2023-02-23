package me.simpleHook.ui.view.about

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp

class AuthorView(context: Context) : CustomViewGroup(context) {
    init {
        val typedValue = TypedValue()
        getContext().theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = getContext().theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        background = typedArray.getDrawable(0)
        isFocusable = true
        isClickable = true
        setPadding(16.dp, 8.dp, 8.dp, 8.dp)
    }

    val avatar = AvatarView(context).apply {
        layoutParams = LayoutParams(50.dp, 50.dp)
        addView(this)
    }

    val name = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_subtitle1)).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(8.dp, 0, 0, 0)
            }
        addView(this)
    }

    val introduce =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_caption)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(0, 8.dp, 0, 0)
                }
            addView(this)
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        this.avatar.autoMeasure()
        val textViewWidth =
            measuredWidth - paddingLeft - paddingRight - this.avatar.measuredWidth - name.marginStart
        name.measure(textViewWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
        introduce.measure(
            textViewWidth.toExactlyMeasureSpec(), introduce.defaultHeightMeasureSpec(this)
        )
        val height = maxOf(
            this.avatar.measuredHeight,
            name.measuredHeightWithMargins + introduce.measuredHeightWithMargins
        )
        setMeasuredDimension(
            measuredWidth, paddingTop + height + paddingBottom
        )
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        this.avatar.autoLayout(paddingLeft, paddingTop)
        name.autoLayout(this.avatar.right + name.marginStart, paddingTop)
        introduce.autoLayout(name.left, name.bottom + introduce.marginTop)
    }
}