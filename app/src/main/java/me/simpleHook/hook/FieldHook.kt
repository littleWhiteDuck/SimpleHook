package me.simpleHook.hook

import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.findAllConstructors
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.hook.language.tip
import me.simpleHook.hook.util.HookHelper.appClassLoader
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.hook.util.LogUtil
import me.simpleHook.hook.util.Type
import me.simpleHook.hook.util.hook
import me.simpleHook.hook.util.isSearchConstructor
import me.simpleHook.hook.util.isSearchMethod

object FieldHook {
    /**
     * @author littleWhiteDuck
     * @param configBean 配置类
     */
    @JvmStatic
    fun hookStaticField(configBean: ConfigBean) {
        configBean.apply {
            if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                // 直接hook
                if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                    recordStaticField(fieldClassName, fieldName)
                } else {
                    hookStaticField(fieldClassName, resultValues, fieldName)
                }
                return
            }
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordStaticField(fieldClassName, fieldName) }
            } else {
                { hookStaticField(fieldClassName, resultValues, fieldName) }
            }
            hookField(hooker)
        }
    }

    private fun ConfigBean.hookField(
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
            LogUtil.outHookError(className, "$methodName($params)", e)
        }
    }

    private fun recordStaticField(
        fieldClassName: String, fieldName: String
    ) {
        val hookClass = XposedHelpers.findClass(fieldClassName, appClassLoader)
        val result = XposedHelpers.getStaticObjectField(hookClass, fieldName)
        val list = listOf(
            tip.className + fieldClassName,
            tip.fieldName + fieldName,
            tip.fieldValue + result
        )
        val logBean =
            LogBean(type = tip.staticField, other = list, packageName = hostPackageName)
        LogUtil.outLogMsg(logBean)
    }

    private fun hookStaticField(
        fieldClassName: String, values: String, fieldName: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, appClassLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, Type.getDataTypeValue(values))
    }

    @JvmStatic
    fun hookInstanceField(
        configBean: ConfigBean
    ) {
        configBean.apply {
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_INSTANCE_FIELD) {
                { recordInstanceField(className, it, fieldName) }
            } else {
                { hookInstanceField(it, resultValues, fieldName) }
            }
            hookField(hooker)
        }
    }

    private fun recordInstanceField(
        className: String, param: XC_MethodHook.MethodHookParam, fieldName: String
    ) {
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, fieldName)
        val list = listOf(
            tip.className + className,
            tip.fieldName + fieldName,
            tip.fieldValue + result
        )
        val logBean =
            LogBean(type = tip.instanceField, other = list, packageName = hostPackageName)
        LogUtil.outLogMsg(logBean)
    }

    private fun hookInstanceField(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, Type.getDataTypeValue(values))
    }
}