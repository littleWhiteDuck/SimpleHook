package me.simpleHook.ui.view.extension

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.marginTop
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.util.dp

class EditSignatureView(context: Context) : CustomViewGroup(context) {
    val editText = TextInputEditText(context).apply {
        background = null
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        maxLines = 5
    }

    private val textInputLayout = TextInputLayout(context).apply {
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        boxStrokeWidth = 2.dp
        setBoxCornerRadii(5f.dp, 5f.dp, 5f.dp, 5f.dp)
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val changeSignButton = MaterialButton(context).apply {
        text = context.getString(R.string.extension_guise_replace_sign_by_apk)
        layoutParams = MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 5.dp, 0, 0)
        }
    }

    init {
        textInputLayout.addView(editText)
        addView(textInputLayout)
        addView(changeSignButton)
        setPadding(5.dp, 0, 5.dp, 0)
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val leftWidth = measuredWidth - paddingStart - paddingEnd
        textInputLayout.measure(
            leftWidth.toExactlyMeasureSpec(), textInputLayout.defaultHeightMeasureSpec(this)
        )
        changeSignButton.measure(
            leftWidth.toExactlyMeasureSpec(), textInputLayout.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            textInputLayout.measuredHeight + changeSignButton.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        textInputLayout.autoLayout(paddingStart, 0)
        changeSignButton.autoLayout(
            changeSignButton.toHorizontalCenter(this),
            textInputLayout.bottom + changeSignButton.marginTop
        )

    }

}