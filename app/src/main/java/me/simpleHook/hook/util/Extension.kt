package me.simpleHook.hook.util

import com.github.kyuubiran.ezxhelper.utils.*
import me.simpleHook.constant.Constant
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun Method.hook(hookMode: Int, hooker: Hooker) {
    when (hookMode) {
        Constant.HOOK_RETURN, Constant.HOOK_RETURN2, Constant.HOOK_PARAM -> hookBefore(hooker)
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS_RETURN -> hookAfter(
            hooker)
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
            hooker)
        Constant.HOOK_BREAK -> this.hookReplace { it.result == null }
    }
}

@JvmName("hookMethod")
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