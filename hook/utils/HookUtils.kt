package me.simpleHook.hook.utils

import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.children
import com.github.kyuubiran.ezxhelper.utils.*
import me.simpleHook.constant.Constant
import me.simpleHook.hook.Tip
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun getAllTextView(viewGroup: ViewGroup): List<String> {
    val list = mutableListOf<String>()
    viewGroup.children.forEach {
        when (it) {
            is Button -> {
                if (it.text.toString().isNotEmpty()) {
                    list.add(Tip.getTip("button") + it.text.toString())
                }
            }
            is TextView -> {
                if (it.text.toString().isNotEmpty()) {
                    list.add(Tip.getTip("text") + it.text.toString())
                }
            }
            is ViewGroup -> {
                list += getAllTextView(it)
            }
        }
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

fun Method.hook(hookMode: Int, hooker: Hooker) {
    when (hookMode) {
        Constant.HOOK_RETURN, Constant.HOOK_RETURN2, Constant.HOOK_PARAM -> hookBefore(hooker)
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS_RETURN -> hookAfter(
            hooker
        )
        Constant.HOOK_BREAK -> this.hookReplace { it.result == null }
    }
}

fun Method.hook(hookBefore: Boolean, hooker: Hooker) {
    if (hookBefore) this.hookBefore(hooker) else hookAfter(hooker)
}

fun List<Method>.hook(hookMode: Int, hooker: Hooker) {
    when (hookMode) {
        Constant.HOOK_RETURN, Constant.HOOK_RETURN2, Constant.HOOK_PARAM -> hookBefore(hooker)
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS_RETURN -> hookAfter(
            hooker
        )
        Constant.HOOK_BREAK -> this.hookReplace { it.result == null }
    }
}

fun List<Method>.hook(hookBefore: Boolean, hooker: Hooker) {
    if (hookBefore) hookBefore(hooker) else hookAfter(hooker)
}

fun Method.isSearchMethod(params: String): Boolean {
    val methodParams = params.split(",")
    val realSize = if (params == "") 0 else methodParams.size
    if (realSize != paramCount) return false
    for (index in 0 until realSize) {
        if (parameterTypes[index].name != Type.getClassTypeName(methodParams[index])) return false
    }
    return true
}

fun Constructor<*>.isSearchConstructor(params: String): Boolean {
    val methodParams = params.split(",")
    val realSize = if (params == "") 0 else methodParams.size
    if (realSize != paramCount) return false
    for (index in 0 until realSize) {
        if (parameterTypes[index].name != Type.getClassTypeName(methodParams[index])) return false
    }
    return true
}