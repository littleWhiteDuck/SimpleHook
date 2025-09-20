package me.simpleHook.util

import kotlinx.serialization.json.Json
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.database.entity.AppConfig
import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {

    private fun isJsonFormat(jsonStr: String): Boolean {
        if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) return true
        if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) return true
        return false
    }

    fun isJsonArray(json: String): Boolean = try {
        if (isJsonFormat(json.trim())) {
            JSONArray(json)
            true
        } else false
    } catch (_: Exception) {
        false
    }

    fun isJsonObject(json: String): Boolean = try {
        if (isJsonFormat(json.trim())) {
            JSONObject(json)
            true
        } else false
    } catch (_: Exception) {
        false
    }


    fun formatJson(jsonStr: String?): String {
        if (null == jsonStr || "" == jsonStr) return ""
        val sb = StringBuilder()
        var last: Char
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

    // TODO optimize
    private fun addIndentBlank(sb: StringBuilder, indent: Int) {
        for (i in 0 until indent) {
            sb.append('\t')
        }
    }

    fun importConfigs(configs: String): List<AppConfigItem2> = runCatching {
        val appConfigs = Json.decodeFromString<List<AppConfig>>(configs)
        val dataList = ArrayList<AppConfigItem2>()
        appConfigs.forEach { appConfig ->
            appConfig.id = 0
            dataList.add(AppConfigItem2(appConfig))
        }
        dataList
    }.getOrDefault(emptyList())
}