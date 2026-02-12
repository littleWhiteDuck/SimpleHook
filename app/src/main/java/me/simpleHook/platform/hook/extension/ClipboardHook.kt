package me.simpleHook.platform.hook.extension

import android.content.ClipData
import android.content.ClipboardManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import me.simpleHook.data.ExClipboardConfig
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.RecordOutHelper

object ClipboardHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.filterClipboard.enable) return
        hookSetClipboard(extensionConfig.filterClipboard)
        hookGetClipboard(extensionConfig.filterClipboard)
    }

    private fun hookGetClipboard(clipboardConfig: ExClipboardConfig) {
        if (clipboardConfig.read || clipboardConfig.record) {
            // hook @getItemAt maybe get better effect
            findMethod(ClipboardManager::class.java) {
                name == "getPrimaryClip"
            }.hookAfter {
                if (clipboardConfig.record) {
                    val clipData = it.result as ClipData?
                    RecordOutHelper.outputClipboard(isRead = true, info = getClipInfo(clipData))
                }
                if (clipboardConfig.read) {
                    it.result = null
                }
            }
        }
    }


    private fun hookSetClipboard(clipboardConfig: ExClipboardConfig) {
        if (clipboardConfig.write || clipboardConfig.record) {
            findMethod(ClipboardManager::class.java) {
                name == "setPrimaryClip"
            }.hookBefore {
                val clipData = it.args[0] as ClipData
                val info = getClipInfo(clipData)
                if (clipboardConfig.record) {
                    RecordOutHelper.outputClipboard(isRead = false, info = info)
                }
                if (clipboardConfig.write) {
                    clipboardConfig.filterKeywords.forEach { keyword ->
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
                val clipDataItem = clipData.getItemAt(i)
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