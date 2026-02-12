package me.simpleHook.feature.config.ui.view

import android.content.Context
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.marginTop
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.core.extension.dp
import me.simpleHook.core.ui.custom.CustomViewGroup

class InputCollectionView(context: Context) : CustomViewGroup(context) {
    val nameEditText = TextInputEditText(context).apply {
        background = null
        hint = context.getString(R.string.config_collection_name_hint)
        setPadding(0, 25.dp, 0, 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private val nameInput = TextInputLayout(ContextThemeWrapper(context, R.style.TextInput)).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10.dp
        }
    }

    val configEditText = TextInputEditText(context).apply {
        background = null
        setPadding(0, 25.dp, 0, 0)
        hint = context.getString(R.string.config_collection_edit_config)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private val configInput = TextInputLayout(context).apply {
        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10.dp
        }
    }

    val insertEnviVar = MaterialButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.config_collection_insert_var_field)
        isEnabled = false
        setOnClickListener {
            val edit = configEditText.editableText
            val start = configEditText.selectionStart
            val end = configEditText.selectionEnd
            if (start < 0 || start >= edit.length) {
                edit.append("\${}")
            } else {
                edit.replace(start, end, "\${}")
            }
            configEditText.setSelection(start + 2)
        }
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        nameInput.addView(nameEditText)
        configInput.addView(configEditText)
        addView(nameInput)
        addView(configInput)
        addView(insertEnviVar)
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val leftWidth = measuredWidth - paddingStart - paddingEnd
        nameInput.measure(leftWidth.toExactlyMeasureSpec(),
            nameInput.defaultHeightMeasureSpec(this))
        configInput.measure(leftWidth.toExactlyMeasureSpec(),
            configInput.defaultHeightMeasureSpec(this))
        insertEnviVar.measure(leftWidth.toExactlyMeasureSpec(),
            insertEnviVar.defaultHeightMeasureSpec(this))
        setMeasuredDimension(measuredWidth,
            nameInput.measuredHeightWithMargins + configInput.measuredHeightWithMargins + insertEnviVar.measuredHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        nameInput.autoLayout(paddingStart, paddingTop + nameInput.marginTop)
        configInput.autoLayout(paddingStart, nameInput.bottom + configInput.marginTop)
        insertEnviVar.autoLayout(paddingStart, configInput.bottom)
    }
}