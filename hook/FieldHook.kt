package me.simpleHook.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object FieldHook {
    fun hookStaticField(
        className: String, classLoader: ClassLoader,
        fieldName: String, values: String, valueType: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
        when (val value = Type.getDataTypeValue(values)) {
            is Byte -> XposedHelpers.setStaticByteField(clazz, fieldName, values.toByte())
            is Short -> XposedHelpers.setStaticShortField(
                clazz,
                fieldName,
                value
            )
            is Int -> XposedHelpers.setStaticIntField(clazz, fieldName, values.toInt())
            is Long -> XposedHelpers.setStaticLongField(clazz, fieldName, values.toLong())
            is Float -> XposedHelpers.setStaticFloatField(
                clazz,
                fieldName,
                value
            )
            is Double -> XposedHelpers.setStaticDoubleField(
                clazz,
                fieldName,
                value
            )
            is Boolean -> XposedHelpers.setStaticBooleanField(
                clazz,
                fieldName,
                value
            )
            is String -> XposedHelpers.setStaticObjectField(clazz, fieldName, value)
            else -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
        }

    }

    fun hookField(
        className: String, classLoader: ClassLoader, fieldName: String, values: String,
        valueType: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
        XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val thisObj = param.thisObject
                when (val value = Type.getDataTypeValue(values)) {
                    is Byte -> XposedHelpers.setByteField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Short -> XposedHelpers.setShortField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Int -> XposedHelpers.setIntField(thisObj, fieldName, value)
                    is Long -> XposedHelpers.setLongField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Float -> XposedHelpers.setFloatField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Double -> XposedHelpers.setDoubleField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Boolean -> XposedHelpers.setBooleanField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is String -> XposedHelpers.setObjectField(thisObj, fieldName, value)
                    else -> XposedHelpers.setObjectField(thisObj, fieldName, null)
                }
            }
        })
    }
}