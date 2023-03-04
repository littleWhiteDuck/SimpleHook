package me.simpleHook.hook.extension

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.GuiseSignConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil
import me.simpleHook.util.OSUtils
import me.simpleHook.util.ToolUtils

object SignatureHook : BaseHook() {
    @Suppress("DEPRECATION")
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.signature || (configBean.guiseSign.enable && configBean.guiseSign.info.isNotEmpty())) {
            findMethod("android.app.ApplicationPackageManager") {
                name == "getPackageInfo" && parameterTypes[0] == String::class.java
            }.hookAfter {
                val flag = it.args[1] as Int
                @Suppress("DEPRECATION") if (flag != PackageManager.GET_SIGNING_CERTIFICATES && flag != PackageManager.GET_SIGNATURES) return@hookAfter
                val packInfo = it.result as PackageInfo
                if (configBean.signature) {
                    val items = LogUtil.getStackTrace()
                    val byteArray =
                        if (OSUtils.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                        packInfo.signingInfo.apkContentsSigners[0].toByteArray()
                    } else {
                        @Suppress("DEPRECATION") packInfo.signatures[0].toByteArray()
                    }
                    val md5 = ToolUtils.getDigest(byteArray)
                    val sha1 = ToolUtils.getDigest(byteArray, "SHA-1")
                    val sha256 = ToolUtils.getDigest(byteArray, "SHA-256")
                    val list = listOf("Signature(MD5): $md5",
                        "Signature(SHA-1): $sha1",
                        "Signature(SHA-256): $sha256")
                    val logBean = LogBean("Signature", list + items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
                val signConfigStr = configBean.guiseSign.info
                if (configBean.guiseSign.enable && signConfigStr.contains(packInfo.packageName) && signConfigStr.contains(
                        "true")
                ) {
                    if (OSUtils.atLeastP() && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                        val guiseSignConfigs = Json.decodeFromString<List<GuiseSignConfig>>(signConfigStr)
                        guiseSignConfigs.forEach { config ->
                            if (config.packageName == packInfo.packageName && config.enable) {
                                packInfo.signingInfo.apkContentsSigners[0] = Signature(config.signData)
                                it.result = packInfo
                                return@hookAfter
                            }
                        }
                    } else {
                        val guiseSignConfigs = Json.decodeFromString<List<GuiseSignConfig>>(signConfigStr)
                        guiseSignConfigs.forEach { config ->
                            if (config.packageName == packInfo.packageName && config.enable) {
                                packInfo.signatures[0] = Signature(config.signData)
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