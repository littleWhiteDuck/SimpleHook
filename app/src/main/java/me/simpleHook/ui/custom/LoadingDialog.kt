package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.app.Activity
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

    var parentView: View? = null

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
        val lp = activity.window.attributes
        lp.alpha = 0.7f
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        //  activity.window.attributes = lp
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        popupWindow.isOutsideTouchable = false
        popupWindow.showAtLocation(parentView ?: loadingView, Gravity.CENTER, 0, 0)
    }

    fun dismiss() {
        quickDismiss()
    }

    fun quickDismiss() {
        popupWindow.dismiss()
        val lp = activity.window.attributes
        lp.alpha = 1f
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        //activity.window.attributes = lp
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}