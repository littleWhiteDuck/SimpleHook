package me.simpleHook.util


import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant.HOOK_BREAK
import me.simpleHook.constant.Constant.HOOK_FIELD
import me.simpleHook.constant.Constant.HOOK_PARAM
import me.simpleHook.constant.Constant.HOOK_RECORD_INSTANCE_FIELD
import me.simpleHook.constant.Constant.HOOK_RECORD_PARAMS
import me.simpleHook.constant.Constant.HOOK_RECORD_PARAMS_RETURN
import me.simpleHook.constant.Constant.HOOK_RECORD_RETURN
import me.simpleHook.constant.Constant.HOOK_RECORD_STATIC_FIELD
import me.simpleHook.constant.Constant.HOOK_RETURN
import me.simpleHook.constant.Constant.HOOK_RETURN2
import me.simpleHook.constant.Constant.HOOK_STATIC_FIELD
import me.simpleHook.hook.util.Type


private const val findAndHookMethod = """
    XposedHelpers.findAndHookMethod('类名', runtime.classLoader, "方法名", 参数类型 XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));
"""
private const val isHook = """
if (hostPackageName == 'package_name') {
    // description
    逻辑代码
}
"""

private const val hookStaticField = """
    XposedHelpers.setStaticObjectField(XposedHelpers.findClass("变量类名", runtime.classLoader), "变量名", 变量值);
"""
private const val hookInstanceField = """
    XposedHelpers.setObjectField(param.thisObject, "变量名", 变量值);
"""
private const val recordStaticField = """
    console.log(XposedHelpers.getStaticObjectField(XposedHelpers.findClass("变量类名", runtime.classLoader), "变量名"));
"""
private const val recordInstanceField = """
    console.log(XposedHelpers.getObjectField(param.thisObject, "变量名"));
"""
private const val hookAllMethods = """
    XposedBridge.hookAllMethods(XposedHelpers.findClass("类名", runtime.classLoader), "方法名", XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));
"""
private const val findAndHookConstructor = """
    XposedHelpers.findAndHookConstructor('类名', runtime.classLoader, 参数类型 XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));
"""
private const val hookAllConstructors = """
    XposedBridge.hookAllConstructors(XposedHelpers.findClass("类名", runtime.classLoader), XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));
"""

private const val hostPackageName = "var hostPackageName = runtime.packageName;"

object JsHook {

    fun getStringJSConfig(list: List<ConfigItem>?) = list?.let {
        var result = "$hostPackageName\n\n"
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            val configStr = toJSConfig(appConfig.configs)
            result += isHook.replace("package_name", appConfig.packageName)
                .replace("description", appConfig.description).replace("逻辑代码", configStr) + "\n"
        }
        result
    } ?: ""

    private fun toJSConfig(configStr: String): String {
        val configs = Json.decodeFromString<List<ConfigBean>>(configStr)
        var result = ""
        configs.forEach {
            it.apply {
                val hookMode = getHookMode(methodName, params)
                val temp = when (mode) {
                    HOOK_STATIC_FIELD -> {
                        val staticFieldStr = hookStaticField.replace("变量类名", fieldClassName)
                            .replace("变量名", fieldName).replace(
                                "变量值", getValue(Type.getDataTypeValue(resultValues)).toString()
                            )
                        if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                            "\n${staticFieldStr}\n"
                        } else {
                            val thisResult = if (hookPoint == "before") {
                                hookMode.replace("after_hook", "")
                                    .replace("before_hook", staticFieldStr)
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            } else {
                                hookMode.replace("after_hook", staticFieldStr)
                                    .replace("before_hook", "").replace("类名", className)
                                    .replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            }
                            "\n${thisResult}\n"
                        }
                    }

                    HOOK_FIELD -> {
                        val instanceFieldStr =
                            hookInstanceField.replace("变量名", fieldName).replace(
                                "变量值",
                                getValue(Type.getDataTypeValue(resultValues)).toString()
                            )
                        val thisResult = if (hookPoint == "before") {
                            hookMode.replace("after_hook", "")
                                .replace("before_hook", instanceFieldStr).replace("类名", className)
                                .replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        } else {
                            hookMode.replace("after_hook", instanceFieldStr)
                                .replace("before_hook", "").replace("类名", className)
                                .replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        }
                        "\n${thisResult}\n"
                    }

                    HOOK_RETURN -> {
                        val resultValue =
                            " param.setResult(${getValue(Type.getDataTypeValue(resultValues))});"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", resultValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_PARAM -> {
                        val paramValue = transParamValues(params, resultValues)
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", paramValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_BREAK -> {
                        val resultValue = " param.setResult(null);"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", resultValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_RECORD_RETURN -> {
                        val resultValue = "console.log('返回值: ' + param.getResult());"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_RECORD_PARAMS -> {
                        var resultValue = ""
                        for (i in params.split(",").indices) {
                            resultValue += "console.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_RECORD_PARAMS_RETURN -> {
                        var resultValue = "common.log('返回值: ' + param.getResult());\n\t"
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    HOOK_RECORD_STATIC_FIELD -> {
                        val staticFieldStr = recordStaticField.replace("变量类名", fieldClassName)
                            .replace("变量名", fieldName)
                        if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                            "\n${staticFieldStr}\n"
                        } else {
                            val thisResult = if (hookPoint == "before") {
                                hookMode.replace("after_hook", "")
                                    .replace("before_hook", staticFieldStr)
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            } else {
                                hookMode.replace("after_hook", staticFieldStr)
                                    .replace("before_hook", "").replace("类名", className)
                                    .replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            }
                            "\n${thisResult}\n"
                        }
                    }

                    HOOK_RECORD_INSTANCE_FIELD -> {
                        val instanceFieldStr = recordInstanceField.replace("变量名", fieldName)
                        val thisResult = if (hookPoint == "before") {
                            hookMode.replace("after_hook", "")
                                .replace("before_hook", instanceFieldStr).replace("类名", className)
                                .replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        } else {
                            hookMode.replace("after_hook", instanceFieldStr)
                                .replace("before_hook", "").replace("类名", className)
                                .replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        }
                        "\n${thisResult}\n"
                    }

                    HOOK_RETURN2 -> {
                        val resultValue =
                            " param.setResult(json.gsonStringToClass('$resultValues',XposedHelpers.findClass('$returnClassName', runtime.classLoader)));"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", resultValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    else -> "//未知"
                }
                result += "$temp \n\n"
            }
        }
        return result
    }

    private fun getValue(value: Any?): Any {
        return when (value) {
            is String -> "'$value'"
            null -> "null"
            else -> "${value.javaClass.name}.valueOf('$value')"
        }
    }

    private fun getHookMode(methodName: String, params: String): String {
        return if (methodName == "<init>") {
            if (params == "*") {
                hookAllConstructors
            } else {
                findAndHookConstructor
            }
        } else {
            if (params == "*") {
                hookAllMethods
            } else {
                findAndHookMethod
            }
        }
    }

    private fun transParamValues(params: String, resultValues: String): String {
        val methodParams = params.split(",")
        var result = ""
        for (i in methodParams.indices) {
            if (resultValues.split(",")[i] == "") continue
            val targetValue = getValue(Type.getDataTypeValue(resultValues.split(",")[i]))
            result += "param.args[$i] = $targetValue;"
            if (i != methodParams.size - 1) result += "\n\t"
        }
        return result
    }

    private fun transParams(params: String): String {
        var value = ""
        if (params == "") return ""
        val methodParams = params.split(",")
        methodParams.forEach { param ->
            val classType = Type.getClassType(param)
            value += if (classType == null) {
                "'${param}'"
            } else {
                "'${classType.name}'"
            }
            value += ","
        }
        return value
    }
}
