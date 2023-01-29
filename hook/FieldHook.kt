package me.simpleHook.hook

import com.github.kyuubiran.ezxhelper.utils.*
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.utils.*
import me.simpleHook.hook.utils.HookHelper.appClassLoader
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.log

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
                hookStaticField(fieldClassName, resultValues, fieldName)
                return
            }
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordStaticField(fieldClassName, fieldName) }
            } else {
                { hookStaticField(fieldClassName, resultValues, fieldName) }
            }
            hookField(hooker, hostPackageName)
        }
    }

    private fun ConfigBean.hookField(
        hooker: Hooker, packageName: String
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
        } catch (e: NoSuchMethodError) {
            LogUtil.noSuchMethod(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())

        } catch (e: NoSuchMethodException) {
            LogUtil.noSuchMethod(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            LogUtil.notFoundClass(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            LogUtil.notFoundClass(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }
    }

    private fun recordStaticField(
        fieldClassName: String, fieldName: String
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Static field" else "静态变量"
        val hookClass = XposedHelpers.findClass(fieldClassName, appClassLoader)
        val result = XposedHelpers.getStaticObjectField(hookClass, fieldName)
        val list = listOf(
            getTip("className") + fieldClassName,
            getTip("fieldName") + fieldName,
            getTip("fieldValue") + result
        )
        val logBean = LogBean(type = type, other = list, packageName = hostPackageName)
        LogUtil.toLogMsg(Gson().toJson(logBean), type)
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
            hookField(hooker, hostPackageName)
        }
    }

    private fun recordInstanceField(
        className: String, param: XC_MethodHook.MethodHookParam, fieldName: String
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Instance field" else "实例变量"
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, fieldName)
        val list = listOf(
            getTip("className") + className,
            getTip("fieldName") + fieldName,
            getTip("fieldValue") + result
        )
        val logBean = LogBean(type = type, other = list, packageName = hostPackageName)
        LogUtil.toLogMsg(Gson().toJson(logBean), type)
    }

    private fun hookInstanceField(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, Type.getDataTypeValue(values))
    }
}