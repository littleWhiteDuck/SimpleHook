package me.simpleHook.hook.extension

import com.google.gson.Gson
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getObjectString
import me.simpleHook.hook.util.LogUtil
import org.json.JSONArray
import org.json.JSONObject

object JSONHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.jsonObject) {
            hookJSONObject()
        }
        if (configBean.jsonArray) {
            hookJSONArray()
        }
    }

    private fun hookJSONObject() {
        XposedBridge.hookAllMethods(JSONObject::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON put" else "JSON 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val items = LogUtil.getStackTrace()
                val logBean = LogBean(type, list + items, HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
            }
        })

        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON creation" else "JSON 创建"
                val jsonObject = param.thisObject

                @Suppress("UNCHECKED_CAST")
                val map = XposedHelpers.getObjectField(jsonObject,
                    "nameValuePairs") as LinkedHashMap<String, Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val items = LogUtil.getStackTrace()
                val logBean = LogBean(type, list + items, HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
            }
        })
    }

    private fun hookJSONArray() {

        XposedBridge.hookAllMethods(JSONArray::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray put" else "JSONArray 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val items = LogUtil.getStackTrace()
                val logBean = LogBean(type, list + items, HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
            }
        })

        XposedBridge.hookAllConstructors(JSONArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray creation" else "JSONArray 创建"
                val jsonObject = param.thisObject

                @Suppress("UNCHECKED_CAST")
                val map = XposedHelpers.getObjectField(jsonObject, "values") as List<Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val items = LogUtil.getStackTrace()
                val logBean = LogBean(type, list + items, HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
            }
        })
    }

}