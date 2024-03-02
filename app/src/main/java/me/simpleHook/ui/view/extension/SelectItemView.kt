package me.simpleHook.ui.view.extension


import android.content.Context
import android.graphics.Color
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.google.android.material.materialswitch.MaterialSwitch
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup


class SelectItemView(context: Context) : CustomViewGroup(context) {
    init {
        setBackgroundResource(R.drawable.extension_item_card_bg)
        setPadding(16.dp, 5.dp, 0, 5.dp)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(this)
    }
    val desc =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item_secondary)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(0, 2.dp, 0, 0)
                }
            addView(this)
        }

    val switch = MaterialSwitch(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(5.dp, 10.dp, 15.dp, 10.dp)
        isClickable = false
        setBackgroundColor(Color.TRANSPARENT)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        switch.autoMeasure()
        val textViewWidth =
            measuredWidth - switch.measuredWidthWithMargins - paddingStart - paddingEnd
        title.measure(textViewWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
        desc.measure(textViewWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        val height =
            if (desc.visibility == GONE) paddingTop + title.measuredHeight + paddingBottom else paddingTop + title.measuredHeight + desc.measuredHeightWithMargins + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        title.autoLayout(paddingStart + title.marginLeft, paddingTop)
        desc.autoLayout(title.left, title.bottom + desc.marginTop)
        switch.autoLayout(paddingEnd, switch.toVerticalCenter(this), fromRight = true)
    }
}