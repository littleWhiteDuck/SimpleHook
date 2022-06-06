package me.simpleHook.hook.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip
import me.simpleHook.hook.getAllTextView
import me.simpleHook.util.log

class ClickEventHook(mClassLoader: ClassLoader, mContext: Context) :
    BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        XposedBridge.hookAllMethods(View::class.java, "performClick", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val list = mutableListOf<String>()
                    val type = if (isShowEnglish) "Click Event" else "点击事件"
                    val view = param.thisObject as View
                    val viewType = view.javaClass.name ?: "未获取到"
                    val listenerInfoObject = XposedHelpers.getObjectField(view, "mListenerInfo")
                    val mOnClickListenerObject =
                        XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener")
                    val callbackType = mOnClickListenerObject.javaClass.name
                    val viewId =
                        if (view.id == View.NO_ID) "id：NO ID" else "id： " + Integer.toHexString(view.id)
                    list.add(Tip.getTip("viewType") + viewType)
                    list.add(Tip.getTip("callbackType") + callbackType)
                    list.add(viewId)
                    if (view is TextView) {
                        list.add(Tip.getTip("text") + view.text.toString())
                    } else if (view is ViewGroup) {
                        list += getAllTextView(view)
                    }
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.getStackTrace(), packageName
                        )
                    )
                    LogHook.toLogMsg(mContext, log, packageName, type)
                } catch (e: Exception) {
                    "error: click".log(packageName)
                }
            }
        })
    }
}