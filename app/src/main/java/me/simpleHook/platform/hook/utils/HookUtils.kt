package me.simpleHook.platform.hook.utils

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson


object HookUtils {
    private val gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }

    fun collectViewTexts(view: View): List<String> {
        val result = ArrayList<String>()
        val stack = ArrayDeque<View>()
        stack.addLast(view)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current is TextView) {
                result.add(current.text.toString())
            }
            if (current is ViewGroup) {
                for (i in current.childCount - 1 downTo 0) {
                    stack.addLast(current.getChildAt(i))
                }
            }
        }
        return result
    }

    fun collectViewIds(view: View): List<String> {
        val result = ArrayList<String>()
        val stack = ArrayDeque<View>()
        stack.addLast(view)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current.id != View.NO_ID) {
                result.add(current.id.toString())
            }
            if (current is ViewGroup) {
                for (i in current.childCount - 1 downTo 0) {
                    stack.addLast(current.getChildAt(i))
                }
            }
        }
        return result
    }

    fun byteToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            if (Integer.toHexString(0xFF and b.toInt()).length == 1) {
                sb.append("0")
            }
            sb.append(Integer.toHexString(0xFF and b.toInt()))
        }
        return sb.toString()
    }

    fun toDisplayString(value: Any?): String {
        if (value == null) return "NULL"
        return value as? String
            ?: try {
                gson.toJson(value)
            } catch (_: Throwable) {
                value.javaClass.name
            }
    }
}
