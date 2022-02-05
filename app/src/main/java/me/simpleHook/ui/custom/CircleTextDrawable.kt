package me.simpleHook.ui.custom

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import me.simpleHook.util.dp

class CircleTextDrawable(
    private val size: Float,
    private val text: String,
    private val textColor: Int = "#FF80AB".toColorInt(),
    private val background: Int = "#FCE4EC".toColorInt()
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f.dp
        textSize = size / 2
        textAlign = Paint.Align.CENTER
    }
    private var offsetY = 0
    private val fontMetrics = Paint.FontMetrics()
    private val textBounds = Rect(0, 0, size.toInt(), size.toInt())
    override fun draw(canvas: Canvas) {
        paint.color = background
        canvas.drawCircle(size / 2, size / 2, size / 2, paint)
        paint.getFontMetrics(fontMetrics)
        paint.color = textColor
        paint.style = Paint.Style.FILL
        paint.getTextBounds(text, 0, text.length, textBounds)
        offsetY = (textBounds.top + textBounds.bottom) / 2
        while (textBounds.right - textBounds.left > 0.8 * size) {
            paint.textSize = paint.textSize - 1.5f.dp
            paint.getTextBounds(text, 0, text.length, textBounds)
            offsetY = (textBounds.top + textBounds.bottom) / 2
        }
        canvas.drawText(text, size / 2, size / 2 - offsetY, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return when (paint.alpha) {
            0 -> PixelFormat.TRANSPARENT
            0xff -> PixelFormat.OPAQUE
            else -> PixelFormat.TRANSLUCENT
        }
    }
}