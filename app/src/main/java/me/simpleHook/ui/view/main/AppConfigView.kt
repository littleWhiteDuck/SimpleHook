package me.simpleHook.ui.view.main

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomSwipeCardViewGroup
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp
import kotlin.math.max

class AppConfigView(context: Context) : CustomSwipeCardViewGroup(context) {
    val container = ContainerView(context)


    val editConfig = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).also {
            setPadding(0, 0, 0, 0)
        }
        text = context.getString(R.string.edit)
        setTextColor(Color.WHITE)
        setBackgroundColor("#4F9BFA".toColorInt())
        addView(this)
    }
    val shareConfig = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).also {
            setPadding(0, 0, 0, 0)
        }
        text = context.getString(R.string.share)
        setTextColor(Color.WHITE)
        setBackgroundColor("#DAFFB946".toColorInt())
        addView(this)
    }
    val deleteConfig = AppCompatButton(context).apply {
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
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
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(5.dp, 5.dp, 5.dp, 0)
        }
        cardElevation = 1.dp.toFloat()
        radius = 5.dp.toFloat()
        addView(container)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        isClickable = true
        container.autoMeasure()
        editConfig.measure(
            editConfig.defaultWidthMeasureSpec(this),
            container.measuredHeight.toExactlyMeasureSpec()
        )
        shareConfig.measure(
            shareConfig.defaultWidthMeasureSpec(this),
            container.measuredHeight.toExactlyMeasureSpec()
        )
        deleteConfig.measure(
            deleteConfig.defaultWidthMeasureSpec(this),
            container.measuredHeight.toExactlyMeasureSpec()
        )
        mContentView = container
        mRightMenuWidths =
            editConfig.measuredWidth + deleteConfig.measuredWidth + shareConfig.measuredWidth
        setMeasuredDimension(measuredWidth, container.measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        container.autoLayout(x = 0, y = 0)
        editConfig.autoLayout(container.right, y = 0)
        shareConfig.autoLayout(editConfig.right, y = 0)
        deleteConfig.autoLayout(shareConfig.right, y = 0)
    }


}

class ContainerView(context: Context) : CustomViewGroup(context) {
    val icon = AppCompatImageView(context).apply {
        layoutParams = MarginLayoutParams(40.dp, 40.dp).apply {
            setMargins(10.dp, 10.dp, 10.dp, 10.dp)
        }
        addView(this)
    }

    val appName = AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
        layoutParams = MarginLayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(10.dp, 8.dp, 0, 0)
        }
        addView(this)
    }

    val desc = AppCompatTextView(
        ContextThemeWrapper(
            context, R.style.text_view_item_secondary
        )
    ).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(10.dp, 5.dp, 0, 8.dp)
            }
        addView(this)
    }

    val switch = SwitchCompat(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(0.dp, 0.dp, 5.dp, 0.dp)
            }
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
        addView(this)
    }

    val dragImage = ImageView(context).apply {
        layoutParams = MarginLayoutParams(24.dp, 24.dp).apply {
            setMargins(10.dp, 10.dp, 10.dp, 10.dp)
        }
        setImageResource(R.drawable.ic_drag_menu_24)
        isVisible = false
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        icon.autoMeasure()
        dragImage.autoMeasure()
        switch.measure(
            switch.defaultWidthMeasureSpec(this), switch.defaultHeightMeasureSpec(this)
        )
        val textViewWidth =
            measuredWidth - icon.measuredWidthWithMargins - switch.measuredWidthWithMargins
        appName.measure(
            textViewWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this)
        )
        desc.measure(textViewWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
        val height = max(
            icon.measuredHeightWithMargins,
            appName.measuredHeightWithMargins + desc.measuredHeightWithMargins
        )
        setMeasuredDimension(measuredWidth, height)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        icon.autoLayout(icon.marginLeft, icon.toVerticalCenter(this))
        appName.autoLayout(
            icon.measuredHeightWithMargins + appName.marginLeft, appName.marginTop
        )
        desc.autoLayout(appName.left, appName.bottom + desc.marginTop)
        switch.autoLayout(switch.marginRight, switch.toVerticalCenter(this), fromRight = true)
        dragImage.autoLayout(switch.marginRight, dragImage.toVerticalCenter(this), fromRight = true)
    }
}