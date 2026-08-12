package me.simpleHook.core.ui.view.loading

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.getColorByAttr
import me.simpleHook.core.ui.custom.CustomViewGroup


class LoadingView(context: Context) : CustomViewGroup(context) {
    private val loadingViewSize = 120.dp
    private val progressBarSize = 60.dp

    private val progressBar = ProgressBar(context).apply {
        layoutParams = MarginLayoutParams(progressBarSize, progressBarSize)
        addView(this)
    }

    val tip = AppCompatTextView(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorSecondary))
        addView(this)
    }

    init {
        setPadding(10.dp, 10.dp, 10.dp, 20.dp)
        setBackgroundResource(R.drawable.bg_loading_dialog)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        tip.measure(
            (loadingViewSize - paddingStart - paddingEnd).toAtMostMeasureSpec(),
            tip.defaultHeightMeasureSpec(this)
        )
        progressBar.measure(
            progressBarSize.toExactlyMeasureSpec(), progressBarSize.toExactlyMeasureSpec()
        )
        setMeasuredDimension(loadingViewSize, loadingViewSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        progressBar.autoLayout(progressBar.toHorizontalCenter(this), y = paddingTop)
        tip.autoLayout(tip.toHorizontalCenter(this), y = paddingBottom, fromBottom = true)
    }
}