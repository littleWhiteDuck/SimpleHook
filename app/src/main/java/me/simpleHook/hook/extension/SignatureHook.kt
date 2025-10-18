package me.simpleHook.hook.extension

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import kotlinx.serialization.json.Json
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.GuiseSignConfig
import me.simpleHook.hook.utils.RecordOutHelper
import me.simpleHook.utils.OSUtil

object SignatureHook : BaseHook() {
    @Suppress("DEPRECATION")
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.signature || (extensionConfig.guiseSign.enable && extensionConfig.guiseSign.info.isNotEmpty())) {
            findMethod("android.app.ApplicationPackageManager") {
                name == "getPackageInfo" && parameterTypes[0] == String::class.java
            }.hookAfter {
                val flag = it.args[1] as Int
                if (flag != PackageManager.GET_SIGNING_CERTIFICATES && flag != PackageManager.GET_SIGNATURES) return@hookAfter
                val packInfo = it.result as PackageInfo
                if (extensionConfig.signature) {
                    val byteArray =
                        if (OSUtil.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                            packInfo.signingInfo!!.apkContentsSigners[0].toByteArray()
                        } else {
                            packInfo.signatures!![0].toByteArray()
                        }
                    RecordOutHelper.outputSignature(signByteArray = byteArray)
                }
                val signConfigStr = extensionConfig.guiseSign.info
                if (extensionConfig.guiseSign.enable && signConfigStr.contains(packInfo.packageName) && signConfigStr.contains(
                        "true"
                    )
                ) {
                    if (OSUtil.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                        val guiseSignConfigs =
                            Json.decodeFromString<List<GuiseSignConfig>>(signConfigStr)
                        guiseSignConfigs.forEach { config ->
                            if (config.packageName == packInfo.packageName && config.enable) {
                                packInfo.signingInfo!!.apkContentsSigners[0] =
                                    Signature(config.signData)
                                it.result = packInfo
                                return@hookAfter
                            }
                        }
                    } else {
                        val guiseSignConfigs =
                            Json.decodeFromString<List<GuiseSignConfig>>(signConfigStr)
                        guiseSignConfigs.forEach { config ->
                            if (config.packageName == packInfo.packageName && config.enable) {
                                packInfo.signatures!![0] = Signature(config.signData)
                                it.result = packInfo
                                return@hookAfter
                            }
                        }
                    }
                }
            }
        }
    }
}