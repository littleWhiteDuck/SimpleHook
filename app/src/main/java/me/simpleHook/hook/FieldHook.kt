package me.simpleHook.hook

import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.findAllConstructors
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.constant.Constant
import me.simpleHook.data.HookConfig
import me.simpleHook.hook.utils.HookHelper.appClassLoader
import me.simpleHook.hook.utils.RecordOutHelper
import me.simpleHook.hook.utils.Type
import me.simpleHook.hook.utils.hook
import me.simpleHook.hook.utils.isSearchConstructor
import me.simpleHook.hook.utils.isSearchMethod

object FieldHook {

    fun hookStaticField(hookConfig: HookConfig) {
        with(hookConfig) {
            if (className.isEmpty() || methodName.isEmpty()) {
                // 直接hook
                if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                    recordStaticField(hookConfig = hookConfig)
                } else {
                    hookStaticField(fieldClassName, resultValues, fieldName)
                }
                return
            }
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordStaticField(hookConfig = hookConfig) }
            } else {
                { hookStaticField(fieldClassName, resultValues, fieldName) }
            }
            hookField(hooker)
        }
    }

    private fun HookConfig.hookField(
        hooker: Hooker
    ) {
        val isBeforeHook = hookPoint == "before"
        try {
            if (methodName == "*") {
                findAllMethods(className) {
                    true
                }.hook(isBeforeHook, hooker)
            } else if (params == "*") {
                if (methodName == "<init>") {
                    findAllConstructors(className) {
                        true
                    }.hook(isBeforeHook, hooker)
                } else {
                    findAllMethods(className) {
                        name == methodName
                    }.hook(isBeforeHook, hooker)
                }
            } else {
                if (methodName == "<init>") {
                    findConstructor(className) {
                        isSearchConstructor(params)
                    }.hook(isBeforeHook, hooker)
                } else {
                    findMethod(className) {
                        name == methodName && isSearchMethod(params)
                    }.hook(isBeforeHook, hooker)
                }
            }
        } catch (e: Throwable) {
            RecordOutHelper.outputError(throwable = e, hookConfig = this)
        }
    }

    private fun recordStaticField(hookConfig: HookConfig) {
        val hookClass = XposedHelpers.findClass(hookConfig.fieldClassName, appClassLoader)
        val result = XposedHelpers.getStaticObjectField(hookClass, hookConfig.fieldName)
        RecordOutHelper.outputFieldRecord(filedValue = result, hookConfig = hookConfig)
    }

    private fun hookStaticField(
        fieldClassName: String, values: String, fieldName: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, appClassLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, Type.getDataTypeValue(values))
    }

    fun hookInstanceField(
        hookConfig: HookConfig
    ) {
        with(hookConfig) {
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_INSTANCE_FIELD) {
                { recordInstanceField(param = it, hookConfig = hookConfig) }
            } else {
                { hookInstanceField(param = it, resultValues, fieldName) }
            }
            hookField(hooker)
        }
    }

    private fun recordInstanceField(param: XC_MethodHook.MethodHookParam, hookConfig: HookConfig) {
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, hookConfig.fieldName)
        RecordOutHelper.outputFieldRecord(filedValue = result, hookConfig = hookConfig)
    }

    private fun hookInstanceField(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, Type.getDataTypeValue(values))
    }
}