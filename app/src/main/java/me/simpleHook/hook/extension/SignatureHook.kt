package me.simpleHook.hook.extension

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.google.gson.Gson
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.util.ToolUtils

object SignatureHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean) {
        if (configBean.signature) {
            findMethod("android.app.ApplicationPackageManager") {
                name == "getPackageInfo" && parameterTypes[0] == String::class.java
            }.hookAfter {
                val flag = it.args[1] as Int
                if (flag != PackageManager.GET_SIGNING_CERTIFICATES && flag != PackageManager.GET_SIGNATURES) return@hookAfter
                val packInfo = it.result as PackageInfo
                val items = LogUtil.getStackTrace()
                var md5 = ""
                var sha1 = ""
                var sha256 = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && flag == PackageManager.GET_SIGNING_CERTIFICATES) {
                    val byteArray = packInfo.signingInfo.apkContentsSigners[0].toByteArray()
                    md5 = ToolUtils.getDigest(byteArray)
                    sha1 = ToolUtils.getDigest(byteArray, "SHA-1")
                    sha256 = ToolUtils.getDigest(byteArray, "SHA-256")
                } else {
                    val byteArray2 = packInfo.signatures[0].toByteArray()
                    md5 = ToolUtils.getDigest(byteArray2)
                    sha1 = ToolUtils.getDigest(byteArray2, "SHA-1")
                    sha256 = ToolUtils.getDigest(byteArray2, "SHA-256")
                }
                val list = listOf(
                    "Signature(MD5): $md5", "Signature(SHA-1): $sha1", "Signature(SHA-256): $sha256"
                )
                val logBean = LogBean("Signature", list + items, HookHelper.hostPackageName)
                LogUtil.toLogMsg(Gson().toJson(logBean), logBean.type)
            }
        }
    }

}