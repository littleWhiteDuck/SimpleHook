package me.simpleHook.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ExConfigTag {
    const val RECORD_DIALOG = "popupConfig_recordDialog"
    const val CANCEL_DIALOG = "popupConfig_cancelDialog"
    const val RECORD_POPUP = "popupConfig_recordPopup"
    const val CANCEL_POPUP = "popupConfig_cancelPopup"
    const val BLOCK_DIALOG = "popupConfig_blockDialog_enable"
    const val RECORD_TOAST = "popupConfig_recordToast"

    const val RECORD_JSON_OBJECT = "jsonConfig_recordObject"
    const val RECORD_JSON_ARRAY = "jsonConfig_recordArray"

    const val RECORD_WEB_URL = "webConfig_recordUrl"
    const val ENABLE_WEB_DEBUG = "webConfig_enableDebug"

    const val DISABLE_AG_SENSOR = "sensorConfig_disableAG"
    const val DISABLE_SPORT_SENSOR = "sensorConfig_disableSport"

    const val BASE64 = "algorithmConfig_base64"
    const val MASSAGE_DIGEST = "algorithmConfig_messageDigest"
    const val HMAC = "algorithmConfig_hmac"
    const val CIPHER = "algorithmConfig_cipher"

    const val RECORD_SIGNATURE = "signConfig_recordSignature"
    const val GUISE_SIGN = "signConfig_guiseSign_enable"

    const val BLOCK_EXIT = "exitConfig_enable"

    const val FILE_MONITOR = "fileMonitor_enable"

    const val FILTER_CLIPBOARD = "filterClipboard_enable"

}

@Serializable
data class ExtensionConfig(
    var all: Boolean = false,
    var hookTip: Boolean = false,
    var intent: Boolean = false,
    var hotFix: Boolean = false,
    var vpn: Boolean = false,
    var click: Boolean = false,
    var adb: Boolean = false,
    var application: Boolean = false,
    var contact: Boolean = false,
    val popupConfig: ExPopupConfig = ExPopupConfig(),
    val algorithmConfig: ExAlgorithmConfig = ExAlgorithmConfig(),
    val jsonConfig: ExJsonConfig = ExJsonConfig(),
    val webConfig: ExWebConfig = ExWebConfig(),
    val filterClipboard: ExClipboardConfig = ExClipboardConfig(),
    val sensorConfig: ExSensorConfig = ExSensorConfig(),
    val signConfig: ExSignConfig = ExSignConfig(),
    var fileMonitor: ExtFileMonitorConfig = ExtFileMonitorConfig(),
    var exitConfig: ExtExitConfig = ExtExitConfig(),
    var recordSettings: ExtRecordSettings = ExtRecordSettings()
)

@Serializable
data class ExSignConfig(
    val recordSignature: Boolean = false,
    val guiseSign: EXGuiseSignConfig = EXGuiseSignConfig()
)

@Serializable
data class ExAlgorithmConfig(
    val base64: Boolean = false,
    val messageDigest: Boolean = false,
    val hmac: Boolean = false,
    val cipher: Boolean = false,
)

@Serializable
data class ExPopupConfig(
    val recordToast: Boolean = false,
    val recordDialog: Boolean = false,
    val cancelDialog: Boolean = false,
    val recordPopup: Boolean = false,
    val cancelPopup: Boolean = false,
    val blockDialog: ExtBlockDialog = ExtBlockDialog(),
)

@Serializable
data class ExJsonConfig(
    val recordObject: Boolean = false,
    val recordArray: Boolean = false,
)

@Serializable
data class ExSensorConfig(
    val disableAG: Boolean = false,
    val disableSport: Boolean = false,
)

@Serializable
data class ExWebConfig(
    val recordUrl: Boolean = false,
    val enableDebug: Boolean = false,
)

@Serializable
data class ExtBlockDialog(
    val enable: Boolean = false,
    var keywordEnable: Boolean = false,
    var keywords: List<String> = emptyList(),
    var idEnable: Boolean = false,
    var ids: List<String> = emptyList(),
)

@Serializable
data class ExtExitConfig(
    val enable: Boolean = false,
    var finish: Boolean = false,
    var exit: Boolean = false,
    var kill: Boolean = false,
    var recordCrash: Boolean = false
)

@Serializable
data class ExtRecordSettings(
    val enable: Boolean = false,
    var enableStack: Boolean = true,
    var enableBase64: Boolean = true,
    var enableHex: Boolean = true,
)


@Serializable
data class EXGuiseSignConfig(
    val enable: Boolean = false,
    val signConfigs: List<EXtGuiseSignItem> = emptyList()
)

@Serializable
data class EXtGuiseSignItem(
    var enable: Boolean = false,
    val appName: String,
    val packageName: String,
    var signData: String
)


@Serializable
@SerialName(value = "ClipboardConfig")
data class ExClipboardConfig(
    val enable: Boolean = false,
    var record: Boolean = false,
    var read: Boolean = false,
    var write: Boolean = false,
    var filterKeywords: List<String> = emptyList()
)


@Serializable
@SerialName(value = "FileMonitorConfig")
data class ExtFileMonitorConfig(
    val enable: Boolean = false,
    var createFile: Boolean = false,
    var deleteFile: Boolean = false,
    var inputFile: Boolean = false,
    var outputFile: Boolean = false,
    var assetsFile: Boolean = false,
    var cacheSize: Int = 0
)
