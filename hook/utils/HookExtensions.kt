package me.simpleHook.platform.hook.utils

import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.core.constant.Constant
import java.lang.reflect.Constructor
import java.lang.reflect.Method

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
    if (hookBefore) hookBefore(hooker) else hookAfter(hooker)
}

fun Constructor<*>.hook(hookBefore: Boolean, hooker: Hooker) {
    if (hookBefore) hookBefore(hooker) else hookAfter(hooker)
}

@JvmName("hookConstructor")
fun List<Constructor<*>>.hook(hookBefore: Boolean, hooker: Hooker) {
    if (hookBefore) hookBefore(hooker) else hookAfter(hooker)
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

@JvmName("hookMethod")
fun List<Method>.hook(hookBefore: Boolean, hooker: Hooker) {
    if (hookBefore) hookBefore(hooker) else hookAfter(hooker)
}

fun Method.isSearchMethod(params: String): Boolean {
    val methodParams = parseMethodParams(params)
    val realSize = methodParams.size
    if (realSize != paramCount) return false
    for (index in 0 until realSize) {
        if (parameterTypes[index].name != HookTypeParser.getClassTypeName(methodParams[index])) return false
    }
    return true
}

fun Constructor<*>.isSearchConstructor(params: String): Boolean {
    val methodParams = parseMethodParams(params)
    val realSize = methodParams.size
    if (realSize != paramCount) return false
    for (index in 0 until realSize) {
        if (parameterTypes[index].name != HookTypeParser.getClassTypeName(methodParams[index])) return false
    }
    return true
}

private fun parseMethodParams(params: String): List<String> {
    val normalized = params.trim()
    if (normalized.isEmpty()) return emptyList()
    return normalized.split(",").map { it.trim() }
}

//xposed log
fun String.xLog() {
    XposedBridge.log("simpleHook(${HookHelper.hostPackageName}): $this")
}
