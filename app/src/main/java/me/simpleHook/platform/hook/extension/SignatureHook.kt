package me.simpleHook.platform.hook.extension

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.core.utils.OSUtil

object SignatureHook : BaseHook() {
    @Suppress("DEPRECATION")
    override fun startHook(extensionConfig: ExtensionConfig) {
        val signConfig = extensionConfig.signConfig

        if (signConfig.recordSignature || (signConfig.guiseSign.enable)) {
            findMethod("android.app.ApplicationPackageManager") {
                name == "getPackageInfo" && parameterTypes[0] == String::class.java
            }.hookAfter {
                val flag = it.args[1] as Int
                if (flag != PackageManager.GET_SIGNING_CERTIFICATES && flag != PackageManager.GET_SIGNATURES) return@hookAfter
                val packInfo = it.result as PackageInfo
                if (signConfig.recordSignature) {
                    val byteArray =
                        if (OSUtil.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                            packInfo.signingInfo!!.apkContentsSigners[0].toByteArray()
                        } else {
                            packInfo.signatures!![0].toByteArray()
                        }
                    RecordOutHelper.outputSignature(signByteArray = byteArray)
                }

                val guiseSign = signConfig.guiseSign
                if (guiseSign.enable) {
                    if (OSUtil.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                        guiseSign.signConfigs.forEach { config ->
                            if (config.packageName == packInfo.packageName && config.enable) {
                                packInfo.signingInfo!!.apkContentsSigners[0] =
                                    Signature(config.signData)
                                it.result = packInfo
                                return@hookAfter
                            }
                        }
                    } else {
                        guiseSign.signConfigs.forEach { config ->
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