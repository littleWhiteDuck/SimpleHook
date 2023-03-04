package me.simpleHook.hook.extension

import android.content.ClipData
import android.content.ClipboardManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ClipboardConfig
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil

object ClipboardHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.filterClipboard.enable) return
        val configInfo = configBean.filterClipboard.info
        // old config
        if (!configInfo.startsWith("{") || !configInfo.endsWith("}")) return
        val clipboardConfig = Json.decodeFromString<ClipboardConfig>(configInfo)
        hookSetClipboard(clipboardConfig)
        hookGetClipboard(clipboardConfig)
    }

    private fun hookGetClipboard(clipboardConfig: ClipboardConfig) {
        if (clipboardConfig.read || clipboardConfig.record) {
            // hook @getItemAt maybe get better effect
            findMethod(ClipboardManager::class.java) {
                name == "getPrimaryClip"
            }.hookAfter {
                if (clipboardConfig.record) {
                    val clipData = it.result as ClipData?
                    val info = getClipInfo(clipData)
                    val type = Tip.getTip("getClipboard")
                    val items = listOf(Tip.getTip("clipboardInfo") + info) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
                if (clipboardConfig.read) {
                    it.result = null
                }
            }
        }
    }


    private fun hookSetClipboard(clipboardConfig: ClipboardConfig) {
        if (clipboardConfig.write || clipboardConfig.record) {
            findMethod(ClipboardManager::class.java) {
                name == "setPrimaryClip"
            }.hookBefore {
                val clipData = it.args[0] as ClipData
                val info = getClipInfo(clipData)
                if (clipboardConfig.record) {
                    val type = Tip.getTip("setClipboard")
                    val items = listOf(Tip.getTip("clipboardInfo") + info) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
                if (clipboardConfig.write) {
                    val keywords = Json.decodeFromString<List<String>>(clipboardConfig.filter)
                    keywords.forEach { keyword ->
                        if (keyword == "" || info == "") {
                            it.result = null
                            return@forEach
                        }
                        runCatching {
                            if (info.contains(Regex(keyword))) {
                                it.result = null
                                return@forEach
                            }
                        }.onFailure { _ ->
                            // illegal format, as normal text
                            if (info.contains(keyword)) {
                                it.result = null
                                return@forEach
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getClipInfo(clipData: ClipData?): String {
        val stringBuilder = StringBuilder()
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                // exclude spaces between items
                val clipDataItem = clipData.getItemAt(i);
                if (clipDataItem.text == null) {
                    clipDataItem.intent?.let { intent ->
                        stringBuilder.append(intent.toString())
                    } ?: stringBuilder.append(clipDataItem.uri)
                } else {
                    stringBuilder.append(clipData.getItemAt(i).text.toString())
                }
            }
        }
        return stringBuilder.toString()
    }
}