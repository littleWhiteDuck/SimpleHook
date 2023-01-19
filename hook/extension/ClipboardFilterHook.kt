package me.simpleHook.hook.extension

import android.content.ClipData
import android.content.ClipboardManager
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.Tip

object ClipboardFilterHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (!configBean.filterClipboard.enable) return
        XposedHelpers.findAndHookMethod(
            ClipboardManager::class.java,
            "setPrimaryClip",
            ClipData::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val info = (param.args[0] as ClipData).getItemAt(0).text.toString()
                    val keyWords = configBean.filterClipboard.info.split("\n")
                    keyWords.forEach {
                        if (it.isNotEmpty() && info.isNotEmpty()) {
                            if (info.contains(Regex(it))) {
                                param.result = null
                                val type = Tip.getTip("filterClipboard")
                                val items = LogUtil.getStackTrace()
                                val logBean = LogBean(
                                    type,
                                    arrayListOf(Tip.getTip("clipboardInfo") + info) + items,
                                    packageName
                                )
                                LogUtil.toLogMsg(
                                    Gson().toJson(logBean), packageName, type
                                )
                                return@forEach
                            }
                        }
                    }
                }
            })
    }
}