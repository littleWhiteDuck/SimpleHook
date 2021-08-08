package littleWhiteDuck

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.constant.Constant
import me.simpleHook.util.log
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
private const val selfCheckConfig =
    "{\"appName\":\"simpleHook\",\"packageName\":\"me.simpleHook\",\"mode\":0,\"config\":[{\"className\":\"me.simpleHook.MainActivity\",\"methodName\":\"isModuleLive\",\"resultValues\":\"true\",\"mode\":0,\"params\":\"\"}]}"

@Keep
fun readyHook(param: XC_LoadPackage.LoadPackageParam?) {
    val packageName = param!!.packageName
    val classLoader = param.classLoader
    val selfCheckJSONObject = JSONObject(selfCheckConfig)
    if (packageName == "me.simpleHook") {
        startHook(selfCheckJSONObject, classLoader)
    } else {
        fileHook(packageName, classLoader)
    }
}

fun fileHook(currentPackageName: String, classLoader: ClassLoader) {
    try {
        val text = File("${Constant.CONFIG_DIRECTORY + currentPackageName}/config.json").readText()
        "获取配置成功".log()
        val jsonObject = JSONObject(text)
        val mode = jsonObject.getInt("mode")
        val canUse = jsonObject.getBoolean("canUse")
        if (canUse) {
            if (mode == Constant.HOOK_ORIGIN) {
                "开始hook（普通）".log()
                startHook(jsonObject, classLoader)
            } else {
                "开始hook（加固）".log()
                specialHook(jsonObject)
            }
        }
    } catch (e: FileNotFoundException) {
        "无运行中软件配置，或软件没有储存权限".log()
        "准备使用Context获取配置".log()
        toContextHook(currentPackageName)
    }

}

fun toContextHook(currentPackageName: String) {
    XposedHelpers.findAndHookMethod(
        Application::class.java,
        "attach",
        Context::class.java,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val context: Context = param!!.args[0] as Context
                context.classLoader.also {
                    toHook(currentPackageName, context, it)
                }
            }
        }
    )
}

fun toHook(packageName: String, context: Context, classLoader: ClassLoader) {
    context.contentResolver.query(uri, null, "packageName = ?", arrayOf(packageName), null)?.apply {
        while (moveToNext()) {
            if (getInt(getColumnIndex("canUse")) == 1) {
                val configString = getString(getColumnIndex("app_config"))
                val configJsonObject = JSONObject(configString)
                "配置获取成功(context)".log()
                startHook(configJsonObject, classLoader)
            }
        }
        close()
    } ?: "cursor is null,获取配置失败".log()
}

fun specialHook(jsonObject: JSONObject) {
    XposedHelpers.findAndHookMethod(
        Application::class.java,
        "attach",
        Context::class.java,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val context = param!!.args[0] as Context
                context.classLoader.also {
                    startHook(jsonObject, it)
                }
            }
        }
    )
}


fun startHook(jsonObject: JSONObject, classLoader: ClassLoader) {
    val jsonArray = jsonObject.getJSONArray("config")
    for (i in 0 until jsonArray.length()) {
        val methodJsonObject = jsonArray.getJSONObject(i)
        val mode = methodJsonObject.getInt("mode")
        val className = methodJsonObject.getString("className")
        val values = methodJsonObject.getString("resultValues")
        if (mode == Constant.HOOK_STATIC_FIELD) {
            val fieldName = methodJsonObject.getString("fieldName")
            val valueType = methodJsonObject.getString("fieldType")
            hookStaticField(className, classLoader, fieldName, values, valueType)
        } else {
            val methodName = methodJsonObject.getString("methodName")
            val params = methodJsonObject.getString("params")
            hook(className, classLoader, methodName, values, params, mode)
        }
    }
}

fun hookStaticField(
    className: String,
    classLoader: ClassLoader,
    fieldName: String,
    values: String,
    valueType: String
) {
    val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
    when (valueType) {
        "boolean" -> XposedHelpers.setStaticBooleanField(clazz, fieldName, values.toBoolean())
        "int" -> XposedHelpers.setStaticIntField(clazz, fieldName, values.toInt())
        "long" -> XposedHelpers.setStaticLongField(clazz, fieldName, values.toLong())
        "float" -> XposedHelpers.setStaticFloatField(clazz, fieldName, values.toFloat())
        "double" -> XposedHelpers.setStaticDoubleField(clazz, fieldName, values.toDouble())
        "null" -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
    }

}


fun hook(
    className: String,
    classLoader: ClassLoader,
    methodName: String, values: String,
    params: String, mode: Int
) {
    val methodParams = params.split(",")
    val realSize = if (params == "") 0 else methodParams.size
    val obj = arrayOfNulls<Any>(realSize + 1)
    for (i in methodParams.indices) {
        val classType = Type.getClassType(methodParams[i])
        if (classType == null) {
            obj[i] = methodParams[i]
        } else {
            obj[i] = classType
        }
    }
    when (mode) {
        Constant.HOOK_RETURN -> {
            obj[realSize] = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = Type.getDataTypeValue(values)
                }
            }
        }
        Constant.HOOK_BREAK -> {
            obj[realSize] = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
        }
        Constant.HOOK_PARAM -> {
            obj[realSize] = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    for (i in methodParams.indices) {
                        if (methodParams[i] == "") continue
                        param.args[i] = Type.getDataTypeValue(values.split(",")[i])
                    }
                }
            }
        }
    }
    XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
}