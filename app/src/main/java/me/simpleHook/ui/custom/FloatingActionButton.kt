package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.simpleHook.R
import me.simpleHook.util.dp

@SuppressLint("ResourceType")
class FloatingActionButton(context: Context, attrs: AttributeSet) :
    CustomViewGroup(context, attrs) {
    private var viewAlpha = 0f
        set(value) {
            field = value
            refreshShowState()
        }
    private val labelText = AppCompatTextView(context).apply {
        textSize = 4f.dp
    }
    private val cardView = MaterialCardView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.marginEnd = 5.dp
            }
        this.addView(labelText)
        setContentPadding(5.dp, 3.dp, 5.dp, 3.dp)
        isClickable = true
    }
    val actionButton =
        FloatingActionButton(ContextThemeWrapper(context, R.style.FloatButtonTheme), attrs).apply {
            size = FloatingActionButton.SIZE_MINI
            layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

    init {
        this.removeAllViews()
        clipChildren = false
        clipToPadding = false
        val typeValue = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton)
        val floatingActionLabelText = typeValue.getText(R.styleable.FloatingActionButton_fab_label)
        labelText.text = floatingActionLabelText
        val src = typeValue.getDrawable(R.styleable.FloatingActionButton_fab_src)
        src?.let {
            actionButton.setImageDrawable(it)
        }
        val tint = typeValue.getColorStateList(R.styleable.FloatingActionButton_fab_tint)
        actionButton.imageTintList = tint
        cardView.id = R.id.fab_label
        addView(cardView)
        actionButton.id = R.id.fab_fab
        addView(actionButton)
        typeValue.recycle()
        refreshShowState()
    }

    private fun refreshShowState() {
        if (viewAlpha == 0f) {
            cardView.visibility = GONE
            actionButton.visibility = GONE
        } else {
            cardView.visibility = VISIBLE
            actionButton.visibility = VISIBLE
        }
        cardView.alpha = viewAlpha
        actionButton.alpha = viewAlpha
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

    override fun setOnClickListener(l: OnClickListener?) {
        actionButton.setOnClickListener(l)
        cardView.setOnClickListener(l)
    }

}