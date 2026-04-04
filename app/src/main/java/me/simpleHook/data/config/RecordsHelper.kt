package me.simpleHook.data.config

import android.content.Context
import android.provider.DocumentsContract
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.simpleHook.core.App
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.compat.DocumentCompat
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.platform.shizuku.ShizukuFileManager
import me.simpleHook.core.utils.FileUtil
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
    private const val READ_BATCH_SIZE = 200

    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    fun getLock(filePath: String): ReentrantLock {
        return locks.computeIfAbsent(filePath) { ReentrantLock() }
    }

    fun insertRecordsFromFile(context: Context, packageName: String): List<RecordEntity> {
        val records = mutableListOf<RecordEntity>()
        runBlocking {
            ingestRecordsFromFile(context, packageName) { batch ->
                records.addAll(batch)
            }
        }
        return records
    }

    suspend fun ingestRecordsFromFile(
        context: Context,
        packageName: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val recordPath = String.format(format = ConfigConstant.RECORD_PATH, packageName)
        val lock = getLock(recordPath)
        lock.lock()
        try {
            if (GlobalValue.isRootWork) {
                if (rootReadRecords(recordPath, onBatch)) return
            } else if (GlobalValue.isShizukuWork) {
                if (shizukuReadRecords(recordPath, onBatch)) return
            } else {
                if (fileReadRecords(packageName, recordPath, context, onBatch)) return
            }
        } catch (e: Exception) {
            LogUtil.outLog(e.stackTraceToString())
        } finally {
            lock.unlock()
        }
    }


    private suspend fun shizukuReadRecords(
        filePath: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): Boolean {
        if (!ShizukuFileManager.isAvailable) return true
        val file = App.externalCacheDir!!.resolve("logs")
        ShizukuFileManager.service?.copyFile(filePath, file.path)
        ShizukuFileManager.service?.deleteFile(filePath)
        if (!file.canRead()) return true
        file.bufferedReader().useLines {
            emitRecordBatches(it, onBatch)
        }
        file.delete()
        return false
    }

    private suspend fun rootReadRecords(
        filePath: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): Boolean {
        val suFile = SuFile.open(filePath)
        if (!suFile.canRead()) return true
        SuFileInputStream.open(suFile).bufferedReader().useLines {
            emitRecordBatches(it, onBatch)
        }
        suFile.delete()
        return false
    }


    private suspend fun fileReadRecords(
        packageName: String,
        recordPath: String,
        context: Context,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): Boolean {
        if (OSUtil.atLeastR()) {
            val fileUri = DocumentCompat.generateFileUri(packageName, recordPath)
            if (!DocumentCompat.isFileExists(context, fileUri)) return true
            context.contentResolver.openInputStream(fileUri)!!.also { inputStream ->
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                bufferedReader.useLines {
                    emitRecordBatches(it, onBatch)
                }
            }
            DocumentsContract.deleteDocument(context.contentResolver, fileUri)
            return false
        } else {
            if (!FileUtil.isFileExists(recordPath)) return true
            File(recordPath).useLines {
                emitRecordBatches(it, onBatch)
            }
            FileUtil.deleteFile(recordPath)
            return false
        }
    }

    private suspend fun emitRecordBatches(
        lines: Sequence<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val batch = ArrayList<RecordEntity>(READ_BATCH_SIZE)
        for (str in lines) {
            getRecordEntity(str)?.let { recordEntity ->
                batch.add(recordEntity)
                if (batch.size >= READ_BATCH_SIZE) {
                    onBatch(ArrayList(batch))
                    batch.clear()
                }
            }
        }
        if (batch.isNotEmpty()) {
            onBatch(ArrayList(batch))
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
