package me.simpleHook.hook.extension

import android.content.res.AssetManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordFileOpType
import me.simpleHook.hook.utils.RecordOutHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

object FileHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        val fileMonitor = extensionConfig.fileMonitor
        if (!fileMonitor.enable) return
        if (fileMonitor.createFile) {
            findMethod(File::class.java) {
                name == "createNewFile"
            }.hookAfter {
                val file = it.thisObject as File
                if (file.path.contains("simpleHook")) return@hookAfter
                RecordOutHelper.outputFileOperation(
                    fileOpType = RecordFileOpType.Create,
                    path = file.path
                )
            }
        }

        if (fileMonitor.deleteFile) {
            findMethod(File::class.java) {
                name == "delete"
            }.hookAfter {
                val file = it.thisObject as File
                RecordOutHelper.outputFileOperation(
                    fileOpType = RecordFileOpType.Delete,
                    path = file.path
                )
            }
        }
        if (fileMonitor.inputFile) {
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
                val info = copyPartData(fileMonitor.cacheSize, length, offset, data)
                RecordOutHelper.outputFileOperation(
                    fileOpType = RecordFileOpType.Read,
                    path = path,
                    partData = info
                )
            }
        }
        if (fileMonitor.outputFile) {
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
                val info = copyPartData(fileMonitor.cacheSize, length, offset, data)
                RecordOutHelper.outputFileOperation(
                    fileOpType = RecordFileOpType.Write,
                    path = path,
                    partData = info
                )
            }
        }
        if (fileMonitor.assetsFile) {
            findMethod(AssetManager::class.java) {
                name == "open" && paramCount == 2
            }.hookAfter {
                val filePath = it.args[0] as String
                RecordOutHelper.outputFileOperation(
                    fileOpType = RecordFileOpType.Assets,
                    path = filePath
                )
            }
        }
    }

    private fun copyPartData(
        cacheSize: Int, length: Int, offset: Int, data: ByteArray
    ): String? {
        return if (cacheSize == 0) {
            null
        } else if (length - offset <= cacheSize) {
            data.copyOfRange(offset, length).toString(Charset.defaultCharset())
        } else {
            data.copyOfRange(offset, cacheSize).toString(Charset.defaultCharset())
        }
    }
}