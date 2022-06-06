package me.simpleHook.hook.extension

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import org.json.JSONArray
import org.json.JSONObject

class JSONHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
        if (configBean.jsonObject) {
            hookJSONObject(packageName)
        }
        if (configBean.jsonArray) {
            hookJSONArray(packageName)
        }
    }

    private fun hookJSONObject(packageName: String) {
        XposedBridge.hookAllMethods(JSONObject::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON put" else "JSON 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val items = LogHook.getStackTrace()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
            }
        })

        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON creation" else "JSON 创建"
                val jsonObject = param.thisObject
                val map: LinkedHashMap<String, Any> = XposedHelpers.getObjectField(
                    jsonObject, "nameValuePairs"
                ) as LinkedHashMap<String, Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val items = LogHook.getStackTrace()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    private fun hookJSONArray(packageName: String) {

        XposedBridge.hookAllMethods(JSONArray::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray put" else "JSONArray 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val items = LogHook.getStackTrace()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
            }
        })

        XposedBridge.hookAllConstructors(JSONArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray creation" else "JSONArray 创建"
                val jsonObject = param.thisObject
                val map: List<Any> = XposedHelpers.getObjectField(
                    jsonObject, "values"
                ) as List<Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val items = LogHook.getStackTrace()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    private fun getObjectString(value: Any): String {
        return if (value is String) value else try {
            Gson().toJson(value)
        } catch (e: java.lang.Exception) {
            value.javaClass.name
        }
    }
}