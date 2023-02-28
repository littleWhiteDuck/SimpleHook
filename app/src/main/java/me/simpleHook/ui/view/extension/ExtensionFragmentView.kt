package me.simpleHook.ui.view.extension

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CustomViewGroup

@Suppress("DEPRECATION")
class ExtensionFragmentView(context: Context) : CustomViewGroup(context) {
    val recyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layoutManager = LinearLayoutManager(context)
        clipToPadding = false
    }
    val emptyText = AppCompatTextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.main_home_empty_tip)
        isVisible = false
    }
    val progressBar = ProgressBar(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    val addConfig = FloatingActionButton(context).apply {
        val drawable = resources.getDrawable(R.drawable.ic_add_24)
        imageTintList = AppCompatResources.getColorStateList(context, R.color.white)
        setImageDrawable(drawable)
        contentDescription = context.getString(R.string.config_add_config)
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 20.dp, 20.dp)
            }
    }

    init {
        addView(recyclerView)
        addView(emptyText)
        addView(progressBar)
        addView(addConfig)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        recyclerView.autoMeasure()
        emptyText.autoMeasure()
        progressBar.autoMeasure()
        addConfig.autoMeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        recyclerView.autoLayout(0, 0)
        emptyText.autoLayout(emptyText.toHorizontalCenter(this), emptyText.toVerticalCenter(this))
        progressBar.autoLayout(progressBar.toHorizontalCenter(this),
            progressBar.toVerticalCenter(this))
        addConfig.autoLayout(addConfig.marginEnd,
            addConfig.marginBottom,
            fromRight = true,
            fromBottom = true)

    }
}