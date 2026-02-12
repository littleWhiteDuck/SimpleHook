package me.simpleHook.data.config

import android.content.Context
import android.provider.DocumentsContract
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.serialization.json.Json
import me.simpleHook.core.App
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.compat.DocumentCompat
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.platform.shizuku.ShizukuFileManager
import me.simpleHook.core.utils.FileUtil
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.core.utils.GuiseBase64
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.core.utils.OSUtil
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream

object RecordsHelper {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    fun getLock(filePath: String): ReentrantLock {
        return locks.computeIfAbsent(filePath) { ReentrantLock() }
    }

    fun insertRecordsFromFile(context: Context, packageName: String): List<RecordEntity> {
        val recordPath = String.format(format = ConfigConstant.RECORD_PATH, packageName)
        val lock = getLock(recordPath)
        lock.lock()
        val list = mutableListOf<RecordEntity>()
        return try {
            if (FlavorUtil.rootVersion) {
                if (GlobalValue.isRootWork) {
                    if (rootReadRecords(recordPath, list)) return emptyList()
                } else {
                    if (shizukuReadRecords(recordPath, list)) return emptyList()
                }
            } else {
                if (fileReadRecords(packageName, recordPath, context, list)) return emptyList()
            }
            list
        } catch (e: Exception) {
            LogUtil.outLog(e.stackTraceToString())
            emptyList()
        } finally {
           lock.unlock()
        }
    }


    private fun shizukuReadRecords(
        filePath: String,
        list: MutableList<RecordEntity>
    ): Boolean {
        if (!ShizukuFileManager.isAvailable) return true
        val file = App.externalCacheDir!!.resolve("logs")
        ShizukuFileManager.service?.copyFile(filePath, file.path)
        ShizukuFileManager.service?.deleteFile(filePath)
        if (!file.canRead()) return true
        file.bufferedReader().useLines {
            it.iterator().forEach { str ->
                getRecordEntity(str)?.let { recordEntity ->
                    list.add(recordEntity)
                }
            }
        }
        file.delete()
        return false
    }

    private fun rootReadRecords(
        filePath: String,
        list: MutableList<RecordEntity>
    ): Boolean {
        val suFile = SuFile.open(filePath)
        if (!suFile.canRead()) return true
        SuFileInputStream.open(suFile).bufferedReader().useLines {
            it.iterator().forEach { str ->
                getRecordEntity(str)?.let { recordEntity ->
                    list.add(recordEntity)
                }
            }
        }
        suFile.delete()
        return false
    }


    private fun fileReadRecords(
        packageName: String,
        recordPath: String,
        context: Context,
        list: MutableList<RecordEntity>
    ): Boolean {
        if (OSUtil.atLeastR()) {
            val fileUri = DocumentCompat.generateFileUri(packageName, recordPath)
            if (!DocumentCompat.isFileExists(context, fileUri)) return true
            context.contentResolver.openInputStream(fileUri)!!.also { inputStream ->
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                bufferedReader.useLines {
                    it.iterator().forEach { str ->
                        getRecordEntity(str)?.let { recordEntity ->
                            list.add(recordEntity)
                        }
                    }
                }
            }
            DocumentsContract.deleteDocument(context.contentResolver, fileUri)
            return false
        } else {
            if (!FileUtil.isFileExists(recordPath)) return true
            File(recordPath).useLines {
                it.iterator().forEach { str ->
                    getRecordEntity(str)?.let { recordEntity ->
                        list.add(recordEntity)
                    }
                }
            }
            FileUtil.deleteFile(recordPath)
            return false
        }
    }

    private fun getRecordEntity(recordStr: String): RecordEntity? {
        val jsonRecord = decodeCompressedRecord(recordStr) ?: return null
        return runCatching {
            Json.decodeFromString<RecordEntity>(jsonRecord)
        }.getOrNull()
    }

    private fun decodeCompressedRecord(compressedRecord: String): String? = runCatching {
        val compressedBytes = GuiseBase64.decode(compressedRecord, GuiseBase64.DEFAULT)
        GZIPInputStream(ByteArrayInputStream(compressedBytes)).bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }.getOrNull()
}