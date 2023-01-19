package me.simpleHook.hook

import android.content.Context
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
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
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.log

object FieldHook {
    /**
     * @author littleWhiteDuck
     * @param configBean 配置类
     * @param packageName 目标应用包名
     */
    @JvmStatic
    fun hookStaticField(configBean: ConfigBean, packageName: String) {
        configBean.apply {
            if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                // 直接hook
                hookStaticField(fieldClassName, ezXClassLoader, resultValues, fieldName)
                return
            }
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordStaticField(appContext, fieldClassName, packageName, fieldName) }
            } else {
                { hookStaticField(fieldClassName, ezXClassLoader, resultValues, fieldName) }
            }
            hookField(hooker, packageName)
        }
    }

    private fun ConfigBean.hookField(
        hooker: Hooker, packageName: String
    ) {
        try {
            if (methodName == "*") {
                findAllMethods(className) {
                    true
                }.hook(hookPoint == "before", hooker)
            } else if (params == "*") {
                if (methodName == "<init>") {
                    hookAllConstructorAfter(className, hooker = hooker)
                } else {
                    findAllMethods(className) {
                        name == methodName
                    }.hook(hookPoint == "before", hooker)
                }
            } else {
                if (methodName == "<init>") {
                    findConstructor(className) {
                        isSearchConstructor(params)
                    }.hookAfter(hooker)
                } else {
                    findMethod(className) {
                        name == methodName && isSearchMethod(params)
                    }.hook(hookPoint == "before", hooker)
                }
            }
        } catch (e: NoSuchMethodError) {
            LogUtil.noSuchMethod(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())

        } catch (e: NoSuchMethodException) {
            LogUtil.noSuchMethod(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            LogUtil.notFoundClass(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            LogUtil.notFoundClass(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }
    }

    private fun recordStaticField(
        context: Context, fieldClassName: String, packageName: String, fieldName: String
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Static field" else "静态变量"
        val hookClass = XposedHelpers.findClass(fieldClassName, context.classLoader)
        val result = XposedHelpers.getStaticObjectField(hookClass, fieldName)
        val list = listOf(
            getTip("className") + fieldClassName,
            getTip("fieldName") + fieldName,
            getTip("fieldValue") + result
        )
        val logBean = LogBean(type = type, other = list, packageName = packageName)
        LogUtil.toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    private fun hookStaticField(
        fieldClassName: String, classLoader: ClassLoader, values: String, fieldName: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, classLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, Type.getDataTypeValue(values))
    }

    @JvmStatic
    fun hookInstanceField(
        configBean: ConfigBean, packageName: String
    ) {
        configBean.apply {
            val hooker: Hooker = if (mode == Constant.HOOK_RECORD_STATIC_FIELD) {
                { recordInstanceField(className, packageName, it, fieldName) }
            } else {
                { hookInstanceField(it, resultValues, fieldName) }
            }
            hookField(hooker, packageName)
        }
    }

    private fun recordInstanceField(
        className: String,
        packageName: String,
        param: XC_MethodHook.MethodHookParam,
        fieldName: String
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Instance field" else "实例变量"
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, fieldName)
        val list = listOf(
            getTip("className") + className,
            getTip("fieldName") + fieldName,
            getTip("fieldValue") + result
        )
        val logBean = LogBean(type = type, other = list, packageName = packageName)
        LogUtil.toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    private fun hookInstanceField(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, Type.getDataTypeValue(values))
    }
}