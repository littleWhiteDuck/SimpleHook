package me.simpleHook.hook.extension

import android.content.res.AssetManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import kotlinx.serialization.json.Json
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.FileMonitorConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.language.tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

object FileHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.fileMonitor.enable && configBean.fileMonitor.info.contains("true")) {
            val fileMonitorConfig =
                Json.decodeFromString<FileMonitorConfig>(configBean.fileMonitor.info)
            if (fileMonitorConfig.createFile) {
                findMethod(File::class.java) {
                    name == "createNewFile"
                }.hookAfter {
                    val file = it.thisObject as File
                    if (file.path.contains("simpleHook")) return@hookAfter
                    val items = listOf(tip.path + file.path) + LogUtil.getStackTrace()
                    val logBean = LogBean(type = tip.createFile, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }

            if (fileMonitorConfig.deleteFile) {
                findMethod(File::class.java) {
                    name == "delete"
                }.hookAfter {
                    val file = it.thisObject as File
                    val items = listOf(tip.path + file.path) + LogUtil.getStackTrace()
                    val logBean = LogBean(type = tip.deleteFile, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.inputFile) {
                findMethod(FileInputStream::class.java) {
                    name == "read" && paramCount == 3
                }.hookAfter {
                    val inputStream = it.thisObject as FileInputStream
                    val path = inputStream.getObjectOrNullAs<String>("path", String::class.java)
                        ?: "FileDescriptor"
                    if (path.contains("simpleHook")) return@hookAfter
                    val length = it.args[2] as Int
                    val offset = it.args[1] as Int
                    val data = it.args[0] as ByteArray
                    val info = copyPartData(fileMonitorConfig.cacheSize, length, offset, data)
                    val items = listOf(
                        tip.path + path,
                        tip.info + info
                    ) + LogUtil.getStackTrace()
                    val logBean = LogBean(type = tip.readFile, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.outputFile) {
                findMethod(FileOutputStream::class.java) {
                    name == "write" && paramCount == 3
                }.hookAfter {
                    val outputStream = it.thisObject as FileOutputStream
                    val path = outputStream.getObjectOrNullAs("path", String::class.java)
                        ?: "FileDescriptor"
                    if (path.contains("simpleHook")) return@hookAfter
                    val data = it.args[0] as ByteArray
                    val offset = it.args[1] as Int
                    val length = it.args[2] as Int
                    val info = copyPartData(fileMonitorConfig.cacheSize, length, offset, data)
                    val items = listOf(
                        tip.path + path,
                        tip.info + info
                    ) + LogUtil.getStackTrace()
                    val logBean = LogBean(type = tip.writeFile, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.assetsFile) {
                findMethod(AssetManager::class.java) {
                    name == "open" && paramCount == 2
                }.hookAfter {
                    val filePath = it.args[0] as String
                    val items = listOf(tip.path + filePath) + LogUtil.getStackTrace()
                    val logBean = LogBean(type = tip.readAssets, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
        }
    }

    private fun copyPartData(
        cacheSize: Int, length: Int, offset: Int, data: ByteArray
    ): String {
        return if (cacheSize == 0) {
            tip.notSetCacheSize
        } else if (length - offset <= cacheSize) {
            data.copyOfRange(offset, length).toString(Charset.defaultCharset())
        } else {
            data.copyOfRange(offset, cacheSize).toString(Charset.defaultCharset())
        }
    }
}