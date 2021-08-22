package littleWhiteDuck

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.PopupWindow
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.content.contentValuesOf
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.constant.Constant
import me.simpleHook.util.log
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception


private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
private val printUri = Uri.parse("content://littleWhiteDuck/print_logs")
private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")
private const val selfCheckConfig = "{\"appName\":\"simpleHook\",\"packageName\":\"me.simpleHook\",\"mode\":0,\"config\":[{\"className\":\"me.simpleHook.MainActivity\",\"methodName\":\"isModuleLive\",\"resultValues\":\"true\",\"mode\":0,\"params\":\"\",\"fieldName\":\"\",\"fieldType\":\"\"}]}"

@Keep
fun readyHook(param: XC_LoadPackage.LoadPackageParam?, sharedPreferences: SharedPreferences?) {
    val packageName = param!!.packageName
    val classLoader = param.classLoader
    val selfCheckJSONObject = JSONObject(selfCheckConfig)
    if (packageName == "me.simpleHook") {
        startHook(selfCheckJSONObject, classLoader)
    } else {
        //优先读取文件配置准备hook
        fileHook(packageName, classLoader, sharedPreferences)
        //优先通过context辅助hook：dialog、toast等
        contextAssistHook(packageName)
    }
}

/**
 * @param sharedPreferences 为xml读取配置做准备
 */
fun fileHook(currentPackageName: String, classLoader: ClassLoader, sharedPreferences: SharedPreferences?, fileName:String = "config") {
    try {
        val strConfig = File("${Constant.CONFIG_DIRECTORY + currentPackageName + "/" + fileName}.json").readText()
        "获取配置成功".log()
        readyHook(strConfig, classLoader)
        readyAssistHook(strConfig, null)
    } catch (e: FileNotFoundException) {
        "无运行中软件配置，或软件没有储存权限".log()
        "准备使用xml获取配置".log()
        if (fileName == "config")toXmlHook(currentPackageName, classLoader, sharedPreferences)
    }

}

fun toXmlHook(currentPackageName: String, classLoader: ClassLoader, sharedPreferences: SharedPreferences?) {
    val error = "no have config or error"
    sharedPreferences?.let {
        val strConfig = it.getString(currentPackageName, error)
        if (strConfig == null || strConfig == error) {
            error.log()
            "准备使用Context获取配置".log()
            toContextHook(currentPackageName)
        } else {
            // xml读取配置成功
            readyHook(strConfig, classLoader)
        }
    } ?: error.log()

}

fun readyHook(strConfig: String, classLoader: ClassLoader) {
    val jsonObject = JSONObject(strConfig)
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
}

fun toContextHook(currentPackageName: String) {
    XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java,
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
        val fieldName = methodJsonObject.getString("fieldName")
        val valueType = methodJsonObject.getString("fieldType")
        val methodName = methodJsonObject.getString("methodName")
        val params = methodJsonObject.getString("params")
        when (mode) {
            Constant.HOOK_STATIC_FIELD -> hookStaticField(className, classLoader, fieldName, values, valueType)
            Constant.HOOK_FIELD -> hookField(
                className,
                classLoader,
                fieldName,
                values,
                valueType,
                params
            )
            else -> hook(className, classLoader, methodName, values, params, mode)
        }
    }
}

fun hookStaticField(className: String, classLoader: ClassLoader,
                    fieldName: String, values: String, valueType: String) {
    val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
    when (valueType) {
        "byte" -> XposedHelpers.setStaticByteField(clazz, fieldName, values.toByte())
        "short" -> XposedHelpers.setStaticShortField(clazz, fieldName, values.toShort())
        "int" -> XposedHelpers.setStaticIntField(clazz, fieldName, values.toInt())
        "long" -> XposedHelpers.setStaticLongField(clazz, fieldName, values.toLong())
        "float" -> XposedHelpers.setStaticFloatField(clazz, fieldName, values.toFloat())
        "double" -> XposedHelpers.setStaticDoubleField(clazz, fieldName, values.toDouble())
        "boolean" -> XposedHelpers.setStaticBooleanField(clazz, fieldName, values.toBoolean())
        "null" -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
    }

}

fun hookField(className: String, classLoader: ClassLoader, fieldName: String, values: String,
    valueType: String, params: String) {
    val constructorParams = params.split(",")
    val realSize = if (params == "") 0 else constructorParams.size
    val obj = arrayOfNulls<Any>(realSize + 1)
    for (i in constructorParams.indices) {
        val classType = Type.getClassType(constructorParams[i])
        if (classType == null) {
            obj[i] = constructorParams[i]
        } else {
            obj[i] = classType
        }
    }
    obj[realSize] = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val thisObj = param.thisObject
            when (valueType) {
                "byte" -> XposedHelpers.setByteField(thisObj, fieldName, values.toByte())
                "short" -> XposedHelpers.setShortField(thisObj, fieldName, values.toShort())
                "int" -> XposedHelpers.setIntField(thisObj, fieldName, values.toInt())
                "long" -> XposedHelpers.setLongField(thisObj, fieldName, values.toLong())
                "float" -> XposedHelpers.setFloatField(thisObj, fieldName, values.toFloat())
                "double" -> XposedHelpers.setDoubleField(thisObj, fieldName, values.toDouble())
                "boolean" -> XposedHelpers.setBooleanField(thisObj, fieldName, values.toBoolean())
                "null" -> XposedHelpers.setObjectField(thisObj, fieldName, null)
            }
        }
    }
    XposedHelpers.findAndHookConstructor(className, classLoader, *obj)
}


fun hook(className: String, classLoader: ClassLoader, methodName: String, values: String,
    params: String, mode: Int) {
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


fun contextAssistHook(packageName: String) {
    XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val context: Context = param!!.args[0] as Context
                var config = ""
                context.contentResolver.query(assistUri, null, "packageName = ?", arrayOf(packageName),null)?.apply {
                    while (moveToNext()){
                        config = getString(getColumnIndex("config"))
                    }
                    close()
                }?: fileHook(packageName, context.classLoader, null, "assist")
                if (config == "") return
                context.classLoader.also {
                    readyAssistHook(config, context)
                }
            }
        }
    )
}

fun readyAssistHook(strConfig: String,context: Context?) {
    val jsonObject = JSONObject(strConfig)
    val allSwitch = jsonObject.getBoolean("allSwitch")
    if (!allSwitch)return
    val dialogSwitch = jsonObject.getBoolean("dialogSwitch")
    val toastSwitch = jsonObject.getBoolean("toastSwitch")
    val popupSwitch = jsonObject.getBoolean("popupWindowSwitch")
    val dialogCancel = jsonObject.getBoolean("dialogCancel")
    toHookDialog(context, dialogSwitch, dialogCancel)
    if (toastSwitch){
        toHookToast(context)
    }
    if (popupSwitch){
        toHookPopupWindow(context)
    }
}

fun toHookPopupWindow(context: Context?) {
    XposedBridge.hookAllMethods(
        PopupWindow::class.java,
        "showAtLocation",
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                val type = param?.thisObject?.javaClass?.name ?: "未知"
                val stackTrace = Throwable().stackTrace
                toLogMsg(type, stackTrace, context)
            }
        })
    XposedBridge.hookAllMethods(
        PopupWindow::class.java,
        "showAsDropDown",
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                val type = param?.thisObject?.javaClass?.name ?: "未知"
                val stackTrace = Throwable().stackTrace
                toLogMsg(type, stackTrace, context)
            }
        })
}

fun toHookToast(context: Context?) {
    XposedBridge.hookAllMethods(Toast::class.java, "show", object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            super.beforeHookedMethod(param)
            val type = param?.thisObject?.javaClass?.name ?: "未知"
            val stackTrace = Throwable().stackTrace
            toLogMsg(type, stackTrace, context)
        }
    })
}

fun toHookDialog(context: Context?,isSwitch: Boolean, cancel:Boolean) {
    XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            super.beforeHookedMethod(param)
            if (cancel){
                val dialog = param?.thisObject as Dialog
                dialog.setCancelable(true)
            }
            if (isSwitch){
                val type = param?.thisObject?.javaClass?.name ?: "未知"
                val stackTrace = Throwable().stackTrace
                toLogMsg(type, stackTrace, context)
            }
        }
    })
}


fun toLogMsg(type: String, stackTrace: Array<StackTraceElement>, context: Context?) {
    val str = StringBuilder()
    str.append("{\n\"type\": \"${type}\",\n\"other\": [\n")
    for (i in stackTrace) {
        val className = i.className
        if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                "de.robv.android.xposed") || className.contains("LspHooker") || className.contains("EdHooker") ||className.startsWith("me.weishu")) continue
        str.append("    \"类：${i.className}-->方法：${i.methodName}(line：${i.lineNumber})\",\n")
    }
    val tempStr = str.toString().substring(0, str.toString().length - 2)
    val strLog = "$tempStr  ]\n}"
    XposedBridge.log("\n$strLog")
    try {
        val contentValues = contentValuesOf("packageName" to "test", "log" to strLog)
        context?.let {
            it.contentResolver?.insert(printUri, contentValues)
        }
    }catch (e: Exception){
        "current error when save log".log()
    }
}