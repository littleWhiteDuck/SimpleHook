package me.simpleHook.hook.extension

import android.content.res.AssetManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.FileMonitorConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
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
                    val type = Tip.getTip("createFile")
                    val items = listOf(Tip.getTip("path") + file.path) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }

            if (fileMonitorConfig.deleteFile) {
                findMethod(File::class.java) {
                    name == "delete"
                }.hookAfter {
                    val file = it.thisObject as File
                    val type = Tip.getTip("deleteFile")
                    val items = listOf(Tip.getTip("path") + file.path) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
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
                    val type = Tip.getTip("readFile")
                    val items = listOf(Tip.getTip("path") + path,
                        Tip.getTip("info") + info) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
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
                    val type = Tip.getTip("writeFile")
                    val items = listOf(Tip.getTip("path") + path,
                        Tip.getTip("info") + info) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.assetsFile) {
                findMethod(AssetManager::class.java) {
                    name == "open" && paramCount == 2
                }.hookAfter {
                    val filePath = it.args[0] as String
                    val type = Tip.getTip("readAssets")
                    val items = listOf(Tip.getTip("path") + filePath) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
        }
    }

    private fun copyPartData(
        cacheSize: Int, length: Int, offset: Int, data: ByteArray
    ): String {
        return if (cacheSize == 0) {
            Tip.getTip("notSetCacheSize")
        } else if (length - offset <= cacheSize) {
            data.copyOfRange(offset, length).toString(Charset.defaultCharset())
        } else {
            data.copyOfRange(offset, cacheSize).toString(Charset.defaultCharset())
        }
    }
}