package me.simpleHook.hook

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
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
        packageName: String
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
        obj[realSize] = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val clazz: Class<*> = XposedHelpers.findClass(fieldClassName, classLoader)
                when (val value = Type.getDataTypeValue(values)) {
                    is Byte -> XposedHelpers.setStaticByteField(clazz, fieldName, value)
                    is Char -> XposedHelpers.setStaticCharField(clazz, fieldName, value)
                    is Short -> XposedHelpers.setStaticShortField(clazz, fieldName, value)
                    is Int -> XposedHelpers.setStaticIntField(clazz, fieldName, value)
                    is Long -> XposedHelpers.setStaticLongField(clazz, fieldName, value)
                    is Float -> XposedHelpers.setStaticFloatField(clazz, fieldName, value)
                    is Double -> XposedHelpers.setStaticDoubleField(clazz, fieldName, value)
                    is Boolean -> XposedHelpers.setStaticBooleanField(clazz, fieldName, value)
                    is String -> XposedHelpers.setStaticObjectField(clazz, fieldName, value)
                    else -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
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
            Tip.getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            Tip.getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            Tip.getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }
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
        packageName: String
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
        obj[realSize] = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val thisObj = param.thisObject
                when (val value = Type.getDataTypeValue(values)) {
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
            Tip.getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            Tip.getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            ErrorTool.notFoundClass(
                context, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            Tip.getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }
}