package me.simpleHook.util

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.roundToInt

/*
//toast
fun String.toast(context: Context,duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}
*/

//
fun String.snack(view: View, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(view, this, duration).show()
}

fun String.random(length: Int): String {
    val temp = StringBuilder()
    for (i in 0 until length) {
        temp.append(this[(Math.random() * (this.length - 1)).roundToInt()])
    }
    return temp.toString()
}

//xposed log
fun String.log(packageName: String) {
    XposedBridge.log("simpleHook($packageName): $this")
}

fun String.tip(packageName: String) {
    XposedBridge.log("simpleHook($packageName): $this")
}

fun String.print() {
    XposedBridge.log("\n\n\n\n${JsonUtil.formatJson(this).replace("\u003e", ">")}\n\n\n\n")
}

// 跑马灯
fun TextView.marquee() {
    this.apply {
        isSelected = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        isSingleLine = true
        marqueeRepeatLimit = -1
    }
}

val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )

val Int.dp
    get() = this.toFloat().dp.toInt()

val Float.px
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX, this, Resources.getSystem().displayMetrics
    )

val Int.px
    get() = this.toFloat().px

fun px2dp(pxValue: Int): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}


val Float.sp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics
    )


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
        override fun onAnimationStart(animation: Animator) {
            beginFakeDrag()
        }

        override fun onAnimationEnd(animation: Animator) {
            endFakeDrag()
        }

        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    })
    animator.interpolator = interpolator
    animator.duration = duration
    animator.start()
}

fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun StringBuilder.lineFeesItem(
    list: List<String>, foreground: String, nLine: Int = 0, nLineString: String = ""
): String {
    this.append(foreground)
    list.forEachIndexed { index, s ->
        if (nLine == index) {
            this.append(nLineString)
        }
        this.append(s)
        if (index != list.size - 1) {
            this.append("\n")
        }
    }
    return this.toString()
}

infix fun Int.isContainState(state: Int): Boolean {
    return (this and state) != 0
}

suspend fun fetchJson(url: String) = withContext(Dispatchers.IO) {
    try {
        JSONObject(URL(url).readText())
    } catch (e: Exception) {
        null
    }
}

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    try {
        URL(url).readText()
    } catch (_: Throwable) {
        null
    }
}
