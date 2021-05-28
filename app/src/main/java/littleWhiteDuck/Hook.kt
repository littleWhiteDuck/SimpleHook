package littleWhiteDuck

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.constant.Constant
import org.json.JSONObject

private val uri = Uri.parse("content://littleWhiteDuck/app_configs")

@Keep
fun readyHook(param: XC_LoadPackage.LoadPackageParam?) {
    val packageName = param!!.packageName
    val classLoader = param.classLoader
    XposedHelpers.findAndHookMethod(
        Application::class.java, "attach",
        Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context: Context = param.args[0] as Context
                toHook(packageName, context, classLoader)
            }
        })
}

fun toHook(packageName: String, context: Context, classLoader: ClassLoader) {
    context.contentResolver.query(uri, null, "packageName = ?", arrayOf(packageName), null)?.apply {
        while (moveToNext()) {
            if (getInt(getColumnIndex("canUse")) == 1){
                val configString = getString(getColumnIndex("app_config"))
                val configJsonObject = JSONObject(configString)
                startHook(configJsonObject, classLoader)
/*                when (configJsonObject.getInt("mode")) {
                    ModeConstant.HOOK_ORIGIN -> startHook(configJsonObject, classLoader)
                    ModeConstant.HOOK_360 -> specialHook(
                        configJsonObject,
                        classLoader,
                        "com.stub.StubApp"
                    )
                    ModeConstant.HOOK_TENCENT -> specialHook(
                        configJsonObject,
                        classLoader,
                        "com.wrapper.proxyapplication.WrapperProxyApplication"
                    )
                    ModeConstant.HOOK_OTHER -> specialHook(
                        configJsonObject, classLoader,
                        configJsonObject.getString("application")
                    )
                }*/
            }



        }
        close()
    }
}


fun specialHook(jsonObject: JSONObject, classLoader: ClassLoader, className: String) {
    XposedHelpers.findAndHookMethod(
        className,
        classLoader,
        "attachBaseContext",
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
        val className = methodJsonObject.getString("className")
        val methodName = methodJsonObject.getString("methodName")
        val values = methodJsonObject.getString("resultValues")
        val params = methodJsonObject.getString("params")
        val mode = methodJsonObject.getInt("mode")
        hook(className, classLoader, methodName, values, params, mode)
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
                    param.result = Type.getDataType(values)
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
                        param.args[i] = Type.getDataType(values.split(",")[i])
                    }
                }
            }
        }
    }
    XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
}