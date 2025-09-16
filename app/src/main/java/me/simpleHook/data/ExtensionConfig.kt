package me.simpleHook.data

@kotlinx.serialization.Serializable
data class ExtensionConfig(
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
    val stopDialog: ExtensionItemConfig = ExtensionItemConfig(info = "{}"),
    val filterClipboard: ExtensionItemConfig = ExtensionItemConfig(info = "{}"),
    var application: Boolean = false,
    var signature: Boolean = false,
    var contact: Boolean = false,
    var tip: Boolean = false,
    var disSensorAG: Boolean = false,
    var disSensorSport: Boolean = false,
    var adb: Boolean = false,
    var guiseSign: ExtensionItemConfig = ExtensionItemConfig(info = "[]"),
    var fileMonitor: ExtensionItemConfig = ExtensionItemConfig(info = "{}"),
    var exit: ExtensionItemConfig = ExtensionItemConfig(info = "{}"),
    var record: ExtensionItemConfig = ExtensionItemConfig(info = "{}")
)

@kotlinx.serialization.Serializable
data class ExtensionItemConfig(
    var enable: Boolean = false, var info: String = ""
)


@kotlinx.serialization.Serializable
data class RecordConfig(
    var enableStack: Boolean = true,
    var enableBase64: Boolean = false
)

@kotlinx.serialization.Serializable
data class GuiseSignConfig(
    val packageName: String, val signData: String, val enable: Boolean = false
)


@kotlinx.serialization.Serializable
data class ClipboardConfig(
    var record: Boolean = false,
    var read: Boolean = false,
    var write: Boolean = false,
    var filter: String = ""
)

@kotlinx.serialization.Serializable
data class FileMonitorConfig(
    var createFile: Boolean = false,
    var deleteFile: Boolean = false,
    var inputFile: Boolean = false,
    var outputFile: Boolean = false,
    var assetsFile: Boolean = false,
    var cacheSize: Int = 0
)

@kotlinx.serialization.Serializable
data class DialogCancel(
    var keywordEnable: Boolean = false,
    var keywords: String = "[]",
    var idEnable: Boolean = false,
    var ids: String = "[]"
)


@kotlinx.serialization.Serializable
data class Exit(
    var finish: Boolean = false,
    var exit: Boolean = false,
    var kill: Boolean = false,
    var recordCrash: Boolean = false
)