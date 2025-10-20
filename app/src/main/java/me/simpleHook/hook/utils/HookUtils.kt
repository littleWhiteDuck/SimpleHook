package me.simpleHook.hook.utils

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.google.gson.Gson


object HookUtils {

    fun getViewAllText(view: View): List<String> {
        val list = mutableListOf<String>()
        if (view is TextView) {
            list.add(view.text.toString())
        } else if (view is ViewGroup) {
            list.addAll(view.children.map { getViewAllText(it) }.flatten())
        }
        return list
    }

    fun getViewIds(view: View): List<String> {
        val list = mutableListOf<String>()
        if (view is ViewGroup) {
            list.addAll(view.children.map { getViewIds(it) }.flatten())
        } else {
            if (view.id != View.NO_ID) list.add(view.id.toString())
        }
        return list
    }

    fun byte2Sting(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            if (Integer.toHexString(0xFF and b.toInt()).length == 1) {
                sb.append("0")
            }
            sb.append(Integer.toHexString(0xFF and b.toInt()))
        }
        return sb.toString()
    }

    fun getObjectString(value: Any?): String {
        if (value == null) return "NULL"
        return value as? String
            ?: try {
                Gson().toJson(value)
            } catch (_: Throwable) {
                value.javaClass.name
            }
    }
}