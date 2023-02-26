package me.simpleHook.ui.view.extension

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup

class ModelListView(context: Context) : CustomViewGroup(context) {

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.BottomModelTitle)).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(5.dp, 0, 0, 0)
            }
        text = context.getString(R.string.extension_template_config)
    }

    val clearAll = MaterialButton(ContextThemeWrapper(context, R.style.BottomModelButton)).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.extension_clear_all_template_config)
    }

    val closeButton =
        MaterialButton(ContextThemeWrapper(context, R.style.BottomModelButton)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 10.dp, 0)
                }
            text = context.getString(R.string.extension_close_bottom_dialog)
        }

    val recyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutManager = GridLayoutManager(context, 2)
        clipToPadding = false
        setPadding(0, 0, 0, 20.dp)
    }

    init {
        addView(title)
        addView(clearAll)
        addView(closeButton)
        addView(recyclerView)
        setPadding(15.dp, 5.dp, 15.dp, 20.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        title.autoMeasure()
        clearAll.autoMeasure()
        closeButton.autoMeasure()
        recyclerView.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
            recyclerView.defaultHeightMeasureSpec(this))
        setMeasuredDimension(measuredWidth,
            closeButton.measuredHeightWithMargins + recyclerView.measuredHeightWithMargins + paddingTop + paddingBottom)
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        title.autoLayout(paddingStart + title.marginStart,
            paddingTop + (clearAll.measuredHeight - title.measuredHeight) / 2)
        closeButton.autoLayout(paddingEnd, paddingTop, fromRight = true)
        clearAll.autoLayout(paddingEnd + closeButton.measuredWidthWithMargins,
            closeButton.top,
            true)
        recyclerView.autoLayout(paddingStart, closeButton.bottom + recyclerView.marginTop)
    }
}