package me.simpleHook.ui.view.about

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.annotation.DrawableRes

private val TRANSFER_MODE = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

class AvatarView(context: Context) : View(context) {
    @DrawableRes
    var iconId: Int = 0
    var avatarWidth = 0f
    private val avatarIcon by lazy { getAvatar() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds by lazy {
        RectF(0f, 0f, avatarWidth, avatarWidth)
    }

    override fun onDraw(canvas: Canvas) {
        val saveCount = canvas.saveLayer(bounds, null)
        canvas.drawOval(0f, 0f, avatarWidth, avatarWidth, paint)
        paint.xfermode = TRANSFER_MODE
        canvas.drawBitmap(avatarIcon, 0f, 0f, paint)
        paint.xfermode = null
        canvas.restoreToCount(saveCount)

    }

    private fun getAvatar(): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, iconId, options)
        options.inJustDecodeBounds = false
        options.inDensity = options.outWidth
        options.inTargetDensity = avatarWidth.toInt()
        return BitmapFactory.decodeResource(context.resources, iconId, options)
    }
}