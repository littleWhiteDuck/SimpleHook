package me.simpleHook.hook

import android.content.Context
import android.net.Uri
import androidx.core.content.contentValuesOf
import me.simpleHook.util.log
import me.simpleHook.util.print
import me.simpleHook.util.tip

object LogHook {
    private val PRINT_URI = Uri.parse("content://littleWhiteDuck/print_logs")
    fun toLogMsg(context: Context?, log: String, packageName: String, type: String) {
        try {
            val contentValues =
                contentValuesOf(
                    "packageName" to packageName,
                    "log" to log,
                    "read" to 0,
                    "type" to type
                )
            context?.let {
                it.contentResolver?.insert(PRINT_URI, contentValues)
            }
        } catch (e: Exception) {
            "current error when save log，请尝试simpleHook保持运行，此次log打印在下方".tip()
            log.print()
        }
    }

    fun toStackTrace(
        stackTrace: Array<StackTraceElement>
    ): List<String> {
        val items = mutableListOf<String>()
        var notBug = 0
        for (element in stackTrace) {
            val className = element.className
            if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                    "de.robv.android.xposed"
                ) || className.contains("LspHooker") || className.contains("EdHooker") || className.startsWith(
                    "me.weishu"
                )
            ) continue
            if (notBug == 0) {
                items.add("调用堆栈：")
            }
            notBug++
            items.add("类：${element.className} -->方法：${element.methodName}(line：${element.lineNumber})")
        }
        return items
    }
}