package me.simpleHook.hook.extension

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.LogUtil
import me.simpleHook.extension.log

object ClickEventHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.click) return
        findMethod(View::class.java) {
            name == "performClick"
        }.hookAfter {
            try {
                val list = mutableListOf<String>()
                val type = if (isShowEnglish) "Click Event" else "点击事件"
                val view = it.thisObject as View
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
                LogUtil.outLogMsg(LogBean(type,
                    list + LogUtil.getStackTrace(),
                    HookHelper.hostPackageName))
            } catch (e: Exception) {
                "error: click".log(HookHelper.hostPackageName)
            }
        }
    }
}