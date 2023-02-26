package me.simpleHook.util

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
        type.startsWith("Error") -> "Error"
        type.startsWith("弹窗") -> "弹窗"
        type.startsWith("Dialog") -> "Dialog"
        type.startsWith("PopupWindow") -> "PopupWindow"
        type.contains("剪贴板") -> "剪贴板"
        type.contains("clipboard") -> "Clip"
        type.contains("变量") -> "变量"
        type.contains("field") -> "Field"
        type == "Signature" -> "Sign"
        type.contains("文件") -> "文件"
        type.contains("file") -> "File"
        type == "CrashCaught" -> "Crash"
        type == "错误捕获" -> "Crash"
        else -> type
    }

    fun getSimpleText(type: String) = when {
        type.startsWith("aes", true) -> "AES"
        type.startsWith("des", true) -> "DES"
        type.startsWith("sha", true) -> "SHA"
        type.startsWith("rsa", true) -> "RSA"
        type.startsWith("Hmac", true) -> "Hmac"
        type.startsWith("md5", true) -> "MD5"
        type.startsWith("Error") -> "Error"
        type.startsWith("弹窗") -> "弹窗"
        type.startsWith("Dialog") -> "Dialog"
        type.startsWith("PopupWindow") -> "PopupWindow"
        type.contains("field") -> "Field"
        type.contains("变量") -> "变量"
        else -> type
    }
}