package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.ui.custom.MyFastScroller

object FastScrollerUtil {
    @SuppressLint("UseCompatLoadingForDrawables")
    @JvmStatic
    fun bind(view: RecyclerView): MyFastScroller {
        val resources = view.context.resources
        val verticalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable, null) as StateListDrawable
        val verticalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable, null)
        val horizontalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable, null) as StateListDrawable
        val horizontalTrackDrawable: Drawable =
            resources.getDrawable(R.drawable.line_drawable, null)
        return MyFastScroller(
            view,
            verticalThumbDrawable,
            verticalTrackDrawable,
            horizontalThumbDrawable,
            horizontalTrackDrawable,
            resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.fastscroll_margin)
        )
    }
}