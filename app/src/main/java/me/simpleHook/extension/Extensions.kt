package me.simpleHook.extension

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.simpleHook.util.Popup
import org.json.JSONObject
import java.net.URL
import kotlin.math.roundToInt


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
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics)

val Int.dp
    get() = this.toFloat().dp.toInt()

val Float.px
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,
        this,
        Resources.getSystem().displayMetrics)

@Suppress("unused")
val Int.px
    get() = this.toFloat().px

val Float.sp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics)

val Int.sp
    get() = this.toFloat().sp.toInt()

@Suppress("unused")
fun px2dp(pxValue: Int): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}


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

fun runThread(block: () -> Unit) {
    Thread(block).start()
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
        fetchText(url)?.let { JSONObject(it) }
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


fun ViewGroup.addViews(vararg views: View): ViewGroup {
    views.forEach {
        addView(it)
    }
    return this
}


fun PreferenceGroup.addPreferences(vararg preferences: Preference) {
    preferences.forEach {
        addPreference(it)
    }
}