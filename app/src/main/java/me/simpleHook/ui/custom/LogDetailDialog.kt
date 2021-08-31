package me.simpleHook.ui.custom

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class LogDetailDialog(context: Context,private val contentView: View) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 26){
            window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }else{
            window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        val attributes = window!!.attributes
        attributes.width = getScreenParams(context)[0]
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
        window!!.attributes = attributes
        setContentView(contentView)

    }

    private fun getScreenParams(context: Context): IntArray {
        val params = IntArray(2)
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outPoint = Point()
        display.getRealSize(outPoint)
        val mRealSizeHeight: Int = outPoint.y //手机屏幕真实高度
        val mRealSizeWidth: Int = outPoint.x //手机屏幕真实宽度
        params[0] = mRealSizeWidth
        params[1] = mRealSizeHeight
        return params
    }
}