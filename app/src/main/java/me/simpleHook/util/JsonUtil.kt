package me.simpleHook.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {
    fun isJsonArray(json: String): Boolean =
        try {
            JSONArray(json)
            true
        } catch (e: Exception) {
            false
        }

    fun isJsonObject(json: String): Boolean =
        try {
            JSONObject(json)
            true
        } catch (e: Exception) {
            false
        }

    fun getElementString(jsonObject: JSONObject, name: String): String =
        try {
            jsonObject.getString(name)
        } catch (e: java.lang.Exception) {
            ""
        }

    fun formatJson(jsonStr: String?): String {
        if (null == jsonStr || "" == jsonStr) return ""
        val sb = StringBuilder()
        var last = '\u0000'
        var current = '\u0000'
        var indent = 0
        var isInQuotationMarks = false
        for (element in jsonStr) {
            last = current
            current = element
            when (current) {
                '"' -> {
                    if (last != '\\') {
                        isInQuotationMarks = !isInQuotationMarks
                    }
                    sb.append(current)
                }
                '{', '[' -> {
                    sb.append(current)
                    if (!isInQuotationMarks) {
                        sb.append('\n')
                        indent++
                        addIndentBlank(sb, indent)
                    }
                }
                '}', ']' -> {
                    if (!isInQuotationMarks) {
                        sb.append('\n')
                        indent--
                        addIndentBlank(sb, indent)
                    }
                    sb.append(current)
                }
                ',' -> {
                    sb.append(current)
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n')
                        addIndentBlank(sb, indent)
                    }
                }
                else -> sb.append(current)
            }
        }
        return sb.toString()
    }

    private fun addIndentBlank(sb: StringBuilder, indent: Int) {
        for (i in 0 until indent) {
            sb.append('\t')
        }
    }

    fun importConfigs(configs: String): List<ConfigItem> = try {
        val type = object : TypeToken<List<AppConfig>>() {}.type
        val appConfigs = Gson().fromJson<List<AppConfig>>(configs, type)
        val dataList = ArrayList<ConfigItem>()
        appConfigs.forEach { appConfig ->
            appConfig.id = 0
            dataList.add(ConfigItem(appConfig))
        }
        dataList
    } catch (e: java.lang.Exception) {
        emptyList()
    }
}