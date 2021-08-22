package me.simpleHook.util

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import de.robv.android.xposed.XposedBridge

//toast
fun String.toast(context: Context,duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}

//
fun String.snack(view: View, duration: Int = Snackbar.LENGTH_SHORT){
    Snackbar.make(view, this, duration).show()
}

//xposed log
fun String.log(){
    XposedBridge.log("===${this}===")
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