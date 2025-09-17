package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.simpleHook.R
import me.simpleHook.extension.dp
import androidx.core.content.withStyledAttributes

@SuppressLint("ResourceType")
class FloatingActionButton(context: Context, attrs: AttributeSet) :
    CustomViewGroup(context, attrs) {
    private var actionViewAlpha = 0f
        set(value) {
            field = value
            refreshShowState()
        }
    private val labelText = AppCompatTextView(context).apply {
        textSize = 4f.dp
        isClickable = false
    }
    private val cardView = MaterialCardView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.marginEnd = 5.dp
            }
        this.addView(labelText)
        setContentPadding(6.dp, 4.dp, 6.dp, 4.dp)
        isClickable = false
    }
    val actionButton =
        FloatingActionButton(ContextThemeWrapper(context, R.style.FloatButtonTheme), attrs).apply {
            size = FloatingActionButton.SIZE_MINI
            layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            isClickable = false
        }

    init {
        this.removeAllViews()
        clipChildren = false
        clipToPadding = false
        context.withStyledAttributes(attrs, R.styleable.FloatingActionButton) {
            val floatingActionLabelText = getText(R.styleable.FloatingActionButton_fab_label)
            labelText.text = floatingActionLabelText
            val src = getDrawable(R.styleable.FloatingActionButton_fab_src)
            src?.let {
                actionButton.setImageDrawable(it)
            }
            //val tint = typeValue.getColorStateList(R.styleable.FloatingActionButton_fab_tint)
            //actionButton.imageTintList = tint
            cardView.id = R.id.fab_label
            addView(cardView)
            actionButton.id = R.id.fab_fab
            addView(actionButton)
            isClickable = true
        }
        refreshShowState()
    }

    private fun refreshShowState() {
        isVisible = actionViewAlpha != 0f
        alpha = actionViewAlpha
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        cardView.autoMeasure()
        actionButton.autoMeasure()
        setMeasuredDimension(
            cardView.measuredWidthWithMargins + actionButton.measuredWidth,
            maxOf(cardView.measuredHeightWithMargins, actionButton.measuredHeightWithMargins)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        cardView.autoLayout(0, cardView.toVerticalCenter(this))
        actionButton.autoLayout(0, actionButton.toVerticalCenter(this), true)
    }

}