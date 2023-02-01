package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
data class ExtensionConfigBean(
    var all: Boolean = false,
    var dialog: Boolean = false,
    var diaCancel: Boolean = false,
    var popup: Boolean = false,
    var popCancel: Boolean = false,
    var toast: Boolean = false,
    var intent: Boolean = false,
    var hotFix: Boolean = false,
    var vpn: Boolean = false,
    var click: Boolean = false,
    var digest: Boolean = false,
    var hmac: Boolean = false,
    var crypt: Boolean = false,
    var base64: Boolean = false,
    var jsonObject: Boolean = false,
    var jsonArray: Boolean = false,
    var webLoadUrl: Boolean = false,
    var webDebug: Boolean = false,
    val stopDialog: ExtensionItemConfig = ExtensionItemConfig(),
    val filterClipboard: ExtensionItemConfig = ExtensionItemConfig(),
    var application: Boolean = false,
    var signature: Boolean = false,
    var contact: Boolean = false,
    var tip: Boolean = false,
    var disSensorAG: Boolean = false,
    var disSensor: Boolean = false
)

@Keep
data class ExtensionItemConfig(
    var enable: Boolean = false, var info: String = ""
)