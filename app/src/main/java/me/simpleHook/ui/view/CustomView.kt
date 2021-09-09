package me.simpleHook.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import me.simpleHook.util.dp

class CustomView(context: Context) : View(context) {
    init {
        layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, 50f.dp, 50f.dp, 10f.dp, 10f.dp, paint)
        canvas.drawCircle(25f.dp, 25f.dp, 15f.dp, paint2)
    }
}