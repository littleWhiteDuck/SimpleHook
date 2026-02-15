package me.simpleHook.platform.hook.extension

import android.util.Log
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordClickEvent
import me.simpleHook.data.record.RecordType
import me.simpleHook.platform.hook.utils.HookUtils
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.xLog

object ClickEventHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.click) return
        findMethod(View::class.java) {
            name == "performClick"
        }.hookAfter {
            try {
                val view = it.thisObject as View
                val listenerInfoObject = XposedHelpers.getObjectField(view, "mListenerInfo")
                val mOnClickListenerObject =
                    XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener")
                val callbackType = mOnClickListenerObject?.javaClass?.name ?: "NULL"
                val viewId = Integer.toHexString(view.id).takeIf { view.id != View.NO_ID }
                RecordOutHelper.outputRecord(
                    type = RecordType.ClickEvent, RecordClickEvent(
                        viewType = view.javaClass.name ?: "NULL",
                        callbackType = callbackType,
                        viewId = viewId,
                        textList = HookUtils.collectViewTexts(view),
                        stackDetail = RecordOutHelper.getStackTraceStr()
                    )
                )
            } catch (e: Exception) {
                "error: click".xLog()
                Log.e("SimpleHook", "startHook: click-event", e)
            }
        }
    }
}