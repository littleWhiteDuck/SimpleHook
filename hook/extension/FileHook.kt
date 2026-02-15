package me.simpleHook.platform.hook.extension

import android.content.res.AssetManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordFileOpType
import me.simpleHook.platform.hook.utils.RecordOutHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.math.min

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
                val readLength = (it.result as? Int) ?: return@hookAfter
                if (readLength <= 0) return@hookAfter
                val offset = it.args[1] as Int
                val data = it.args[0] as ByteArray
                val info = copyPartData(fileMonitor.cacheSize, readLength, offset, data)
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
        if (cacheSize == 0) return null
        if (offset < 0 || offset >= data.size || length <= 0) return ""
        val availableLength = data.size - offset
        val effectiveLength = min(length, availableLength)
        if (effectiveLength <= 0) return ""
        val cappedLength = min(cacheSize, effectiveLength)
        val endIndex = offset + cappedLength
        return data.copyOfRange(offset, endIndex).toString(Charset.defaultCharset())
    }
}
