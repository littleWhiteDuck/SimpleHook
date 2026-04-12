package me.simpleHook.platform.lsposed

import android.os.ParcelFileDescriptor
import io.github.libxposed.service.XposedService
import me.simpleHook.core.constant.ConfigConstant
import java.nio.ByteBuffer

object LSPosedHelper {
    private var service: XposedService? = null

    fun setService(service: XposedService?) {
        this.service = service
    }

    /**
     * 判断模块是否已在LSPosed new api下激活
     * API 101直接看是否能获取到service即可
     * API 100的部分版本会存在总开关关闭但还可以获取到service的情况，这时候通过scope粗糙判断，并不准确
     */
    fun isActivated() = service != null

    fun filterNotInScope(packageNames: List<String>): List<String> {
        val scope = service?.scope ?: return packageNames
        return packageNames.filter { !scope.contains(it) }
    }

    fun requestScopes(packageNames: List<String>) {
        val filtered = filterNotInScope(packageNames) // 避免已经在作用域还请求授权
        if (filtered.isEmpty()) return
        try {
            service?.requestScope(
                filtered,
                object : XposedService.OnScopeEventListener {})
        } catch (e: XposedService.ServiceException) {
            e.printStackTrace()
        }
    }

    fun removeScopes(packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        try {
            service?.removeScope(packageNames)
        } catch (e: XposedService.ServiceException) {
            e.printStackTrace()
        }
    }

    suspend fun writeConfig(pkgName: String, config: String): Boolean {
        return writeCustomConfig(pkgName, config)
    }

    suspend fun writeCustomConfig(pkgName: String, config: String): Boolean {
        return writeRemoteConfig(ConfigConstant.customRemoteConfigFileName(pkgName), config)
    }

    suspend fun writeExtensionConfig(pkgName: String, config: String): Boolean {
        return writeRemoteConfig(ConfigConstant.extensionRemoteConfigFileName(pkgName), config)
    }

    suspend fun deleteCustomConfig(pkgName: String): Boolean {
        return deleteRemoteConfig(ConfigConstant.customRemoteConfigFileName(pkgName))
    }

    suspend fun deleteExtensionConfig(pkgName: String): Boolean {
        return deleteRemoteConfig(ConfigConstant.extensionRemoteConfigFileName(pkgName))
    }

    private fun writeRemoteConfig(fileName: String, config: String): Boolean {
        service ?: return false
        return runCatching {
            val parcelFileDescriptor = service!!.openRemoteFile(fileName)
            ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptor).use { outputStream ->
                outputStream.channel.use { fileChannel ->
                    fileChannel.truncate(0)
                    fileChannel.write(ByteBuffer.wrap(config.toByteArray()))
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun deleteRemoteConfig(fileName: String): Boolean {
        service ?: return false
        return runCatching {
            service!!.deleteRemoteFile(fileName)
        }.getOrDefault(false)
    }

}
