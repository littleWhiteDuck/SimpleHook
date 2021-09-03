package me.simpleHook.util

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import de.robv.android.xposed.XposedBridge
import me.simpleHook.BuildConfig

/*
//toast
fun String.toast(context: Context,duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}
*/

//
fun String.snack(view: View, duration: Int = Snackbar.LENGTH_SHORT){
    Snackbar.make(view, this, duration).show()
}

//xposed log
fun String.log(){
    if (BuildConfig.DEBUG){
        XposedBridge.log("===${this}===")
    }
}

fun String.tip(){
    XposedBridge.log("*****===${this}===*****")
}

// 跑马灯
fun TextView.marquee(){
    this.apply {
        isSelected = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        isSingleLine = true
        marqueeRepeatLimit = -1
    }
}

val Float.dp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

val Int.dp
    get() = this.toFloat().dp

val Float.px
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, this, Resources.getSystem().displayMetrics)

val Int.px
    get() = this.toFloat().px



fun ViewPager2.setCurrentItem(
    item: Int,
    duration: Long,
    interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
    pagePxWidth: Int = width
) {
    val pxToDrag: Int = pagePxWidth * (item - currentItem)
    val animator = ValueAnimator.ofInt(0, pxToDrag)
    var previousValue = 0
    animator.addUpdateListener { valueAnimator ->
        val currentValue = valueAnimator.animatedValue as Int
        val currentPxToDrag = (currentValue - previousValue).toFloat()
        fakeDragBy(-currentPxToDrag)
        previousValue = currentValue
    }
    animator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            beginFakeDrag()
        }

        override fun onAnimationEnd(animation: Animator?) {
            endFakeDrag()
        }

        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationRepeat(animation: Animator?) {}
    })
    animator.interpolator = interpolator
    animator.duration = duration
    animator.start()
}

fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    val cardView = CardView(context)
    val textView = TextView(context).apply {
        text = this@toast
        textSize = 18f
        gravity = Gravity.CENTER_VERTICAL
        setPadding(20, 5, 20, 5)
        setTextColor(Color.WHITE)
    }
    cardView.apply {
        addView(textView)
        radius = 25f
        setCardBackgroundColor(Color.parseColor("#4F9BFA"))
    }
    Toast(context).apply {
        view = cardView
        setGravity(Gravity.CENTER, 0, 0)
        setDuration(duration)
        show()
    }
}
