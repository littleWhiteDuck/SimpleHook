package me.simpleHook.hook.extension

import android.content.res.AssetManager
import com.github.kyuubiran.ezxhelper.utils.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.FileMonitorConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean) {
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
                    val items = listOf("Path: " + file.path) + LogUtil.getStackTrace()
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
                    val items = listOf("Path: " + file.path) + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.inputFile) {
                findMethod(FileInputStream::class.java) {
                    name == "read" && paramCount == 3
                }.hookAfter {
                    val inputStream = it.thisObject as FileInputStream
                    val info = inputStream.getObjectOrNullAs<String>("path", String::class.java)
                        ?: inputStream.getObject("fd").toString()
                    if (info.contains("simpleHook")) return@hookAfter
                    val type = Tip.getTip("readFile")
                    val items = listOf("Path/FileDescriptor: $info") + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.outputFile) {
                findMethod(FileOutputStream::class.java) {
                    name == "write" && paramCount == 3
                }.hookAfter {
                    val outputStream = it.thisObject as FileOutputStream
                    val info = outputStream.getObjectOrNullAs("path", String::class.java)
                        ?: outputStream.getObject("fd").toString()
                    if (info.contains("simpleHook")) return@hookAfter
                    val type = Tip.getTip("writeFile")
                    val items = listOf("Path/FileDescriptor: $info") + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
            if (fileMonitorConfig.assetsFile) {
                findMethod(AssetManager::class.java) {
                    name == "open" && paramCount == 2
                }.hookAfter {
                    val fileName = it.args[0] as String
                    val type = Tip.getTip("readAssets")
                    val items = listOf("name: $fileName") + LogUtil.getStackTrace()
                    val logBean = LogBean(type, items, HookHelper.hostPackageName)
                    LogUtil.outLogMsg(logBean)
                }
            }
        }
    }
}