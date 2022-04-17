package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow

@SuppressLint("Range")
class LoadingDialog(private val activity: Activity, showText: String) {
    private var popupWindow: PopupWindow
    private val progressBar: ProgressBar = ProgressBar(activity, null)

    init {
        progressBar.showText = showText
        progressBar.measure(
            View.MeasureSpec.makeMeasureSpec(
                ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.AT_MOST
            ), View.MeasureSpec.makeMeasureSpec(
                ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.AT_MOST
            )
        )
        popupWindow =
            PopupWindow(progressBar, progressBar.measuredWidth, progressBar.measuredHeight)
    }

    fun show() {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        popupWindow.isOutsideTouchable = false
        popupWindow.showAtLocation(progressBar, Gravity.CENTER, 0, 0)
    }

    fun dismiss() {
        popupWindow.dismiss()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}