package me.simpleHook.util

import me.simpleHook.constant.Constant

object RecordType {
    fun getShowText(type: String) = when {
        type.startsWith("sha", true) -> "SHA"
        type.startsWith("aes", true) -> "AES"
        type.startsWith("des", true) -> "DES"
        type.startsWith("rsa", true) -> "RSA"
        type.startsWith("Hmac", true) -> "Hmac"
        type.startsWith("md5", true) -> "MD5"
        type.contains("popupWindow", true) -> "Popup"
        "中断执行|返回值|参数值".contains(type) -> type[0].toString()
        type == "Click Event" -> "Click"
        type.startsWith("JSON ") -> "JSON"
        type.startsWith("JSONArray ") -> "JSONArray"
        type == "Return value" -> "Return"
        type == "Param value" -> "Param"
        type == "Param&Return Value" -> "PR"
        type == Constant.SIMPLE_HOOK_ERROR -> "Error"
        else -> type
    }

    fun getSimpleText(type: String) = when {
        type.startsWith("aes", true) -> "AES"
        type.startsWith("des", true) -> "DES"
        type.startsWith("sha", true) -> "SHA"
        type.startsWith("rsa", true) -> "RSA"
        type.startsWith("Hmac", true) -> "Hmac"
        type.startsWith("md5", true) -> "MD5"
        else -> type
    }
}