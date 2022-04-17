package me.simpleHook.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ProgressBar
import me.simpleHook.R
import me.simpleHook.util.dp

private val TEXT_SIZE = 15.dp.toFloat()
private val HORIZONTAL_OFFSET = 50.dp.toFloat()
private val VERTICAL_OFFSET = 30.dp.toFloat()
class ProgressBar(context: Context, attrs: AttributeSet?) : ProgressBar(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var showText = ""
    init {
        paint.textSize = TEXT_SIZE
        paint.color = context.resources.getColor(R.color.paint_color)
        setPadding(paddingLeft, paddingTop, (paddingRight + TEXT_SIZE * 6).toInt(), paddingBottom)
        setBackgroundResource(R.drawable.popup_shape)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(
            showText.ifEmpty { context.getString(R.string.main_loading) },
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            paint
        )
    }
}