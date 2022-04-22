package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import me.simpleHook.ui.view.loading.LoadingView


@SuppressLint("Range")
class LoadingDialog(private val activity: Activity, loadingTip: String) {
    private var popupWindow: PopupWindow

    //    private val progressBar: ProgressBar = ProgressBar(activity, null)
    private val loadingView = LoadingView(activity)
    private var tempTime = 0L

    init {
        loadingView.tip.text = loadingTip
        loadingView.measure(
            View.MeasureSpec.makeMeasureSpec(
                ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.AT_MOST
            ), View.MeasureSpec.makeMeasureSpec(
                ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.AT_MOST
            )
        )
        popupWindow =
            PopupWindow(loadingView, loadingView.measuredWidth, loadingView.measuredHeight)
    }

    fun show() {
        tempTime = System.currentTimeMillis()
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        popupWindow.isOutsideTouchable = false
        popupWindow.showAtLocation(loadingView, Gravity.CENTER, 0, 0)
    }

    fun dismiss() {
        val currentTime = System.currentTimeMillis()
        val delayTime = if (currentTime - tempTime < 1000) 1000 - (currentTime - tempTime) else 0
        Handler(Looper.getMainLooper()).postDelayed({
            popupWindow.dismiss()
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }, delayTime)
    }

    fun quickDismiss() {
        popupWindow.dismiss()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}