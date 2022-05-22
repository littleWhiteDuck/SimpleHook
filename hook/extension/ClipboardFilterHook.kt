package me.simpleHook.hook.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip

class ClipboardFilterHook(mClassLoader: ClassLoader, mContext: Context) :
    BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
        XposedHelpers.findAndHookMethod(
            ClipboardManager::class.java,
            "setPrimaryClip",
            ClipData::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val info = (param.args[0] as ClipData).getItemAt(0).text.toString()
                    val keyWords =configBean.filterClipboard.info.split("\n")
                    keyWords.forEach {
                        if (it.isNotEmpty() && info.isNotEmpty()) {
                            if (info.contains(Regex(it))) {
                                param.result = null
                                val type = Tip.getTip("filterClipboard")
                                val stackTrace = Throwable().stackTrace
                                val items = LogHook.toStackTrace(stackTrace).toList()
                                val logBean = LogBean(
                                    type,
                                    arrayListOf(Tip.getTip("clipboardInfo") + info) + items,
                                    packageName
                                )
                                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
                                return@forEach
                            }
                        }
                    }
                }
            })
    }
}