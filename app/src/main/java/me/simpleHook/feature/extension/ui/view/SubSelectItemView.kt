package me.simpleHook.feature.extension.ui.view


import android.content.Context
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.google.android.material.materialswitch.MaterialSwitch
import me.simpleHook.R
import me.simpleHook.core.extension.addViews
import me.simpleHook.core.extension.dp
import me.simpleHook.core.ui.custom.CustomViewGroup

@Suppress("DEPRECATION")
class SubSelectItemView(context: Context) : CustomViewGroup(context) {


    val containerView = ContainerView(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    }

    private val lineView = View(context).apply {
        layoutParams = LayoutParams(2.dp, LayoutParams.WRAP_CONTENT)
        setBackgroundColor(context.resources.getColor(R.color.divider_color))
    }
    val switch = MaterialSwitch(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setPadding(5.dp, 10.dp, 15.dp, 10.dp)
    }

    init {
        addViews(containerView, lineView, switch)
        //  setPadding(16.dp, 5.dp, 16.dp, 5.dp)

        setBackgroundColor(context.resources.getColor(R.color.extension_item_card_normal))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        switch.autoMeasure()
        val leftWidth =
            measuredWidth - switch.measuredWidthWithMargins - lineView.measuredWidth - paddingStart - paddingEnd
        containerView.measure(leftWidth.toExactlyMeasureSpec(),
            containerView.defaultHeightMeasureSpec(this))
        val height =
            maxOf(containerView.measuredHeightWithMargins, switch.measuredHeightWithMargins)
        lineView.measure(2.dp.toExactlyMeasureSpec(),
            (switch.measuredHeightWithMargins - 10.dp).toExactlyMeasureSpec())
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        containerView.autoLayout(paddingStart, containerView.toVerticalCenter(this))
        switch.autoLayout(paddingEnd + switch.marginEnd,
            switch.toVerticalCenter(this),
            fromRight = true)
        lineView.autoLayout(switch.measuredWidthWithMargins + paddingEnd,
            lineView.toVerticalCenter(this),
            fromRight = true)
    }

}


class ContainerView(context: Context) : CustomViewGroup(context) {
    init {
        setBackgroundResource(R.drawable.extension_item_card_bg)
        setPadding(16.dp, 5.dp, 5.dp, 5.dp)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val leftWidth = measuredWidth - paddingStart - paddingEnd
        title.measure(leftWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
        desc.measure(leftWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        val height =
            title.measuredHeight + desc.measuredHeightWithMargins + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        title.autoLayout(paddingStart + title.marginLeft, paddingTop)
        desc.autoLayout(title.left, title.bottom + desc.marginTop)
    }
}