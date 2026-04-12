package me.simplehook.plugin.old.hook

import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.paramCount
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun Method.hook(hookMode: Int, hooker: Hooker) {
    when (hookMode) {
        Constant.HOOK_RETURN, Constant.HOOK_RETURN2, Constant.HOOK_PARAM -> hookBefore(hooker)
        Constant.HOOK_BREAK -> hookReplace { it.result == null }
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
        Constant.HOOK_BREAK -> hookReplace { it.result == null }
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
        if (parameterTypes[index].name != Type.getClassTypeName(methodParams[index])) return false
    }
    return true
}

fun Constructor<*>.isSearchConstructor(params: String): Boolean {
    val methodParams = parseMethodParams(params)
    val realSize = methodParams.size
    if (realSize != paramCount) return false
    for (index in 0 until realSize) {
        if (parameterTypes[index].name != Type.getClassTypeName(methodParams[index])) return false
    }
    return true
}

private fun parseMethodParams(params: String): List<String> {
    val normalized = params.trim()
    if (normalized.isEmpty()) return emptyList()
    return normalized.split(",").map { it.trim() }
}

fun String.xLog() {
    val hostPackageName = runCatching { InitFields.hostPackageName }.getOrDefault("unknown")
    XposedBridge.log("simpleHook($hostPackageName): $this")
}
