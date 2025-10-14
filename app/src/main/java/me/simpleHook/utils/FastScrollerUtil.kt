package me.simpleHook.utils

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.extension.getColorByAttr
import me.simpleHook.ui.custom.MyFastScroller

object FastScrollerUtil {
    @SuppressLint("UseCompatLoadingForDrawables")
    @JvmStatic
    fun bind(view: RecyclerView): MyFastScroller {
        val resources = view.context.resources
        val verticalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable, null) as StateListDrawable
        verticalThumbDrawable.setTint(view.context.getColorByAttr(android.R.attr.colorPrimary))
        val verticalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable, null)
        val horizontalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable, null) as StateListDrawable
        horizontalThumbDrawable.setTint(view.context.getColorByAttr(android.R.attr.colorPrimary))
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