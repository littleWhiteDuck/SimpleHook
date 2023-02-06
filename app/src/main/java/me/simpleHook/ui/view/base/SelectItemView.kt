package me.simpleHook.ui.view.base

import android.content.Context
import android.widget.CheckBox
import androidx.core.view.marginEnd
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp

class SelectItemView(context: Context) : CustomViewGroup(context) {

    val checkBox = CheckBox(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 5.dp, 0)
            }
        isClickable = false
        addView(this)
    }

    val itemText = CheckBox(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        isClickable = false
        addView(this)
    }

    init {
        setPadding(5.dp, 0, 0, 5.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkBox.autoMeasure()
        itemText.measure(
            (measuredWidth - paddingStart - paddingEnd - checkBox.measuredWidthWithMargins).toExactlyMeasureSpec(),
            itemText.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(measuredWidth, maxOf(checkBox.measuredHeight, itemText.measuredHeight))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        checkBox.autoLayout(paddingStart, checkBox.toVerticalCenter(this))
        itemText.autoLayout(checkBox.right + checkBox.marginEnd, itemText.toVerticalCenter(this))
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        checkBox.isChecked = !checkBox.isChecked
    }
}