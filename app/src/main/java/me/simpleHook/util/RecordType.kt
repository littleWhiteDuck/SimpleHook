package me.simpleHook.util

object RecordType {
    fun getShowText(type: String) = when {
        type.startsWith("sha", true) -> "SHA"
        type.startsWith("aes", true) -> "AES"
        type.startsWith("des", true) -> "DES"
        type.startsWith("rsa", true) -> "RSA"
        type.startsWith("Hmac", true) -> "Hmac"
        type.contains("popupWindow", true) -> "Popup"
        "中断执行|返回值|参数值".contains(type) -> type[0].toString()
        else -> type
    }

    fun getSimpleText(type: String) = when {
        type.startsWith("aes", true) -> "AES"
        type.startsWith("des", true) -> "DES"
        type.startsWith("sha", true) -> "SHA"
        type.startsWith("rsa", true) -> "RSA"
        type.startsWith("Hmac", true) -> "Hmac"
        else -> type
    }
}