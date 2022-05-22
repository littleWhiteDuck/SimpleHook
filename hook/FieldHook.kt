package me.simpleHook.hook

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.log

object FieldHook {
    /**
     * @author littleWhiteDuck
     * @param className Hook变量之前hook的方法所在的类的类名
     * @param classLoader
     * @param methodName Hook变量之前hook的方法的方法名
     * @param fieldName 变量名
     * @param values 要修改的变量值
     * @param fieldClassName 变量所在的类的类名
     * @param context
     * @param packageName 应用包名
     */
    @JvmStatic
    fun hookStaticField(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        params: String,
        fieldName: String,
        values: String,
        fieldClassName: String,
        context: Context,
        packageName: String,
        hookPoint: String,
        isRecord: Boolean
    ) {
        if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
            // 直接hook
            hookStaticField(fieldClassName, classLoader, values, fieldName)
            return
        }
        val methodParams = params.split(",")
        val realSize = if (params == "" || params == "*") 0 else methodParams.size
        val obj = arrayOfNulls<Any>(realSize + 1)
        for (i in 0 until realSize) {
            val classType = Type.getClassType(methodParams[i])
            if (classType == null) {
                obj[i] = methodParams[i]
            } else {
                obj[i] = classType
            }
        }
        obj[realSize] = if (hookPoint == "before") {
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (isRecord) {
                        recordStaticField(context, fieldClassName, packageName, fieldName)
                    } else {
                        hookStaticField(fieldClassName, classLoader, values, fieldName)
                    }
                }
            }
        } else {
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (isRecord) {
                        recordStaticField(context, fieldClassName, packageName, fieldName)
                    } else {
                        hookStaticField(fieldClassName, classLoader, values, fieldName)
                    }
                }
            }
        }

        try {
            if (params == "*") {
                val hookClass = classLoader.loadClass(className)
                if (methodName == "<init>") {
                    XposedBridge.hookAllConstructors(hookClass, obj[realSize] as XC_MethodHook?)
                } else {
                    XposedBridge.hookAllMethods(
                        hookClass, methodName, obj[realSize] as XC_MethodHook?
                    )
                }
            } else {
                if (methodName == "<init>") {
                    XposedHelpers.findAndHookConstructor(className, classLoader, *obj)
                } else {
                    XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
                }
            }
        } catch (e: NoSuchMethodError) {
            ErrorTool.noSuchMethod(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
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
        val list = listOf(getTip("className") + fieldClassName, getTip("fieldName") + fieldName, getTip("fieldValue") + result)
        val logBean = LogBean(type = type, other = list, packageName = packageName)
        LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
    }

    private fun hookStaticField(
        fieldClassName: String, classLoader: ClassLoader, values: String, fieldName: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, classLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, Type.getDataTypeValue(values))
    }

    @JvmStatic
    fun hookField(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        params: String,
        fieldName: String,
        values: String,
        context: Context,
        packageName: String,
        hookPoint: String,
        isRecord: Boolean
    ) {
        val methodParams = params.split(",")
        val realSize = if (params == "" || params == "*") 0 else methodParams.size
        val obj = arrayOfNulls<Any>(realSize + 1)
        for (i in 0 until realSize) {
            val classType = Type.getClassType(methodParams[i])
            if (classType == null) {
                obj[i] = methodParams[i]
            } else {
                obj[i] = classType
            }
        }
        obj[realSize] = if (hookPoint == "before") {
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isRecord) {
                        recordInstanceField(context, className, packageName, param, fieldName)
                    } else {
                        hookInstanceField(param, values, fieldName)
                    }
                }
            }
        } else {
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (isRecord) {
                        recordInstanceField(context, className, packageName, param, fieldName)
                    } else {
                        hookInstanceField(param, values, fieldName)
                    }
                }
            }
        }
        try {
            if (params == "*") {
                val hookClass = classLoader.loadClass(className)
                if (methodName == "<init>") {
                    XposedBridge.hookAllConstructors(hookClass, obj[realSize] as XC_MethodHook?)
                } else {
                    XposedBridge.hookAllMethods(
                        hookClass, methodName, obj[realSize] as XC_MethodHook?
                    )
                }
            } else {
                if (methodName == "<init>") {
                    XposedHelpers.findAndHookConstructor(className, classLoader, *obj)
                } else {
                    XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
                }
            }
        } catch (e: NoSuchMethodError) {
            ErrorTool.noSuchMethod(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }

    private fun recordInstanceField(
        context: Context,
        className: String,
        packageName: String,
        param: XC_MethodHook.MethodHookParam,
        fieldName: String
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Instance field" else "实例变量"
        val thisObj = param.thisObject
        val result = XposedHelpers.getObjectField(thisObj, fieldName)
        val list = listOf(getTip("className") + className, getTip("fieldName") + fieldName, getTip("fieldValue") + result)
        val logBean = LogBean(type = type, other = list, packageName = packageName)
        LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
    }

    private fun hookInstanceField(
        param: XC_MethodHook.MethodHookParam, values: String, fieldName: String
    ) {
        val thisObj = param.thisObject
        XposedHelpers.setObjectField(thisObj, fieldName, Type.getDataTypeValue(values))
        /* when (val value = x) {
             is Byte -> XposedHelpers.setByteField(
                 thisObj, fieldName, value
             )
             is Char -> XposedHelpers.setCharField(thisObj, fieldName, value)
             is Short -> XposedHelpers.setShortField(
                 thisObj, fieldName, value
             )
             is Int -> XposedHelpers.setIntField(thisObj, fieldName, value)
             is Long -> XposedHelpers.setLongField(
                 thisObj, fieldName, value
             )
             is Float -> XposedHelpers.setFloatField(
                 thisObj, fieldName, value
             )
             is Double -> XposedHelpers.setDoubleField(
                 thisObj, fieldName, value
             )
             is Boolean -> XposedHelpers.setBooleanField(
                 thisObj, fieldName, value
             )
             is String -> XposedHelpers.setObjectField(thisObj, fieldName, value)
             else -> XposedHelpers.setObjectField(thisObj, fieldName, null)
         }*/
    }
}