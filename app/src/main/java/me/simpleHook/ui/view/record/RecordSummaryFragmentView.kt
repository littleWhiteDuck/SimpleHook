package me.simpleHook.ui.view.record

import android.content.Context
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup

class RecordSummaryFragmentView(context: Context) : CustomViewGroup(context) {
    val progressBar = LinearProgressIndicator(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        isIndeterminate = true
    }

    val emptyTip = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.main_record_empty_tip)
        gravity = Gravity.CENTER
    }

    val swipeRefreshLayout = SwipeRefreshLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    val recyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        clipToPadding = false
        setPadding(0, 0, 0, 50.dp)
    }

    init {
        swipeRefreshLayout.addView(recyclerView)
        addView(progressBar)
        addView(emptyTip)
        addView(swipeRefreshLayout)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        progressBar.autoMeasure()
        emptyTip.autoMeasure()
        swipeRefreshLayout.autoMeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        progressBar.autoLayout(0, 0)
        emptyTip.autoLayout(emptyTip.toHorizontalCenter(this), emptyTip.toVerticalCenter(this))
        swipeRefreshLayout.autoLayout(0, 0)
    }
}