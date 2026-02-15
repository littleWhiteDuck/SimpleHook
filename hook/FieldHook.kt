package me.simpleHook.platform.hook

import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.findAllConstructors
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.HookConfig
import me.simpleHook.platform.hook.utils.HookHelper.appClassLoader
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.HookTypeParser
import me.simpleHook.platform.hook.utils.hook
import me.simpleHook.platform.hook.utils.isSearchConstructor
import me.simpleHook.platform.hook.utils.isSearchMethod

object FieldHook {

    fun hookStaticField(hookConfig: HookConfig) {
        with(hookConfig) {
            if (className.isEmpty() || methodName.isEmpty()) {
                if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                    recordStaticField(hookConfig = hookConfig)
                } else {
                    setStaticFieldValue(fieldClassName, resultValues, fieldName)
                }
                return
            }
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordStaticField(hookConfig = hookConfig) }
            } else {
                { setStaticFieldValue(fieldClassName, resultValues, fieldName) }
            }
            attachFieldHook(hooker)
        }
    }

    private fun HookConfig.attachFieldHook(
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
        RecordOutHelper.outputFieldRecord(fieldValue = result, hookConfig = hookConfig)
    }

    private fun setStaticFieldValue(
        fieldClassName: String, values: String, fieldName: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, appClassLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, HookTypeParser.getDataTypeValue(values))
    }

    fun hookInstanceField(
        hookConfig: HookConfig
    ) {
        with(hookConfig) {
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_INSTANCE_FIELD) {
                { recordInstanceField(param = it, hookConfig = hookConfig) }
            } else {
                { setInstanceFieldValue(param = it, resultValues, fieldName) }
            }
            attachFieldHook(hooker)
        }
    }

    private fun recordInstanceField(param: XC_MethodHook.MethodHookParam, hookConfig: HookConfig) {
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, hookConfig.fieldName)
        RecordOutHelper.outputFieldRecord(fieldValue = result, hookConfig = hookConfig)
    }

    private fun setInstanceFieldValue(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, HookTypeParser.getDataTypeValue(values))
    }
}
