package me.simpleHook.ui.view.backup

import android.content.Context
import android.graphics.Color
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup
import kotlin.math.max

class RestoreItemView(context: Context) : CustomViewGroup(context) {
    val icon = AppCompatImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(16.dp, 10.dp, 10.dp, 10.dp)
        addView(this)
    }
    val desc = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(0, 10.dp, 16.dp, 0)
        setTextColor(Color.BLACK)
        addView(this)
    }

    val time = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundResource(R.drawable.extension_item_card_bg)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        icon.autoMeasure()
        val leftWidth = measuredWidth - icon.measuredWidth
        desc.measure(leftWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        time.measure(leftWidth.toExactlyMeasureSpec(), time.defaultHeightMeasureSpec(this))
        val height = max(icon.measuredHeight, desc.measuredHeight + time.measuredHeight)
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        icon.autoLayout()
        desc.autoLayout(icon.right)
        time.autoLayout(icon.right, desc.bottom)
    }
}