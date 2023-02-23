package me.simpleHook.ui.view.config

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import me.simpleHook.extension.dp


class RoundBackgroundColorSpan(private val bgColor: Int, private val textColor: Int) :
    ReplacementSpan() {
    private val radius = 3f.dp
    private val padding = 5.dp


    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int, fm: FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt() + padding * 2
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.color = bgColor
        canvas.drawRoundRect(
            RectF(
                x,
                (top + 1).toFloat(),
                x + paint.measureText(text, start, end).toInt() + padding * 2,
                (bottom - 1).toFloat()
            ), radius, radius, paint
        )
        paint.color = textColor
        canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
    }
}