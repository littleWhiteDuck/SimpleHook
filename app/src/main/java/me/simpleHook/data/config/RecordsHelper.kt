package me.simpleHook.data.config

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.simpleHook.core.App
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.compat.DocumentCompat
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.FileUtil
import me.simpleHook.core.utils.GuiseBase64
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.core.utils.OSUtil
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.platform.shizuku.ShizukuFileManager
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream

object RecordsHelper {
    private const val READ_BATCH_SIZE = 200
    private const val MAX_RECORDS_PER_RUN = 2000
    private const val MAX_SEGMENTS_PER_RUN = 8
    private const val MAX_RUN_MILLIS = 1500L

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
        val readyPath = ConfigConstant.RECORD_QUEUE_READY_DIR.formatPath(packageName)
        val lock = getLock(readyPath)
        lock.lock()
        try {
            if (GlobalValue.isRootWork) {
                rootReadSegments(packageName, readyPath, onBatch)
            } else if (GlobalValue.isShizukuWork) {
                shizukuReadSegments(packageName, readyPath, onBatch)
            } else {
                fileReadSegments(packageName, readyPath, context, onBatch)
            }
        } catch (e: Exception) {
            LogUtil.outLog(e.stackTraceToString())
        } finally {
            lock.unlock()
        }
    }

    private suspend fun shizukuReadSegments(
        packageName: String,
        readyPath: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val service = ShizukuFileManager.service ?: return
        val names = service.listFiles(readyPath)
            ?.filter { it.endsWith(".seg") }
            ?.sorted()
            ?.take(MAX_SEGMENTS_PER_RUN)
            ?: return
        if (names.isEmpty()) return

        val cacheDir = App.externalCacheDir!!.resolve("record_segments").also { it.mkdirs() }
        val start = System.currentTimeMillis()
        var consumedRecords = 0

        for (name in names) {
            if (shouldStop(start, consumedRecords)) break
            val localFile = File(cacheDir, name)
            val remotePath = "$readyPath/$name"
            if (service.copyFile(remotePath, localFile.path) && localFile.canRead()) {
                val complete = localFile.inputStream().use { input ->
                    emitRecordBatches(
                        input = input,
                        packageName = packageName,
                        segmentName = name,
                        onBatch = onBatch
                    )
                }
                consumedRecords += complete.recordsRead
                localFile.delete()
                service.deleteFile(remotePath)
            }
        }
    }

    private suspend fun rootReadSegments(
        packageName: String,
        readyPath: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val readyDir = SuFile.open(readyPath)
        val segments = readyDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".seg") }
            ?.sortedBy { it.name }
            ?.take(MAX_SEGMENTS_PER_RUN)
            ?: return
        val start = System.currentTimeMillis()
        var consumedRecords = 0

        for (segment in segments) {
            if (shouldStop(start, consumedRecords)) break
            val result = SuFileInputStream.open(segment).use { input ->
                emitRecordBatches(
                    input = input,
                    packageName = packageName,
                    segmentName = segment.name,
                    onBatch = onBatch
                )
            }
            consumedRecords += result.recordsRead
            segment.delete()
        }
    }

    private suspend fun fileReadSegments(
        packageName: String,
        readyPath: String,
        context: Context,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        if (OSUtil.atLeastR()) {
            documentReadSegments(packageName, readyPath, context, onBatch)
        } else {
            localFileReadSegments(packageName, readyPath, onBatch)
        }
    }

    private suspend fun documentReadSegments(
        packageName: String,
        readyPath: String,
        context: Context,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val rootUri = Constant.ANDROID_DATA_URI.toUriOrNull() ?: return
        val childPath = readyPath.removePrefix(Constant.ANDROID_DATA_PATH)
        val readyDir = DocumentCompat.getDocumentFile(context, rootUri, childPath) ?: return
        val segments = readyDir.listFiles()
            .filter { it.isFile && it.name?.endsWith(".seg") == true }
            .sortedBy { it.name.orEmpty() }
            .take(MAX_SEGMENTS_PER_RUN)

        val start = System.currentTimeMillis()
        var consumedRecords = 0
        for (segment in segments) {
            if (shouldStop(start, consumedRecords)) break
            val name = segment.name ?: continue
            val input = context.contentResolver.openInputStream(segment.uri) ?: continue
            val result = input.use {
                emitRecordBatches(
                    input = it,
                    packageName = packageName,
                    segmentName = name,
                    onBatch = onBatch
                )
            }
            consumedRecords += result.recordsRead
            DocumentsContract.deleteDocument(context.contentResolver, segment.uri)
        }
    }

    private suspend fun localFileReadSegments(
        packageName: String,
        readyPath: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val segments = File(readyPath).listFiles { file -> file.isFile && file.extension == "seg" }
            ?.sortedBy { it.name }
            ?.take(MAX_SEGMENTS_PER_RUN)
            ?: return
        val start = System.currentTimeMillis()
        var consumedRecords = 0

        for (segment in segments) {
            if (shouldStop(start, consumedRecords)) break
            val result = segment.inputStream().use { input ->
                emitRecordBatches(
                    input = input,
                    packageName = packageName,
                    segmentName = segment.name,
                    onBatch = onBatch
                )
            }
            consumedRecords += result.recordsRead
            FileUtil.deleteFile(segment)
        }
    }

    private suspend fun emitRecordBatches(
        input: InputStream,
        packageName: String,
        segmentName: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): SegmentReadResult {
        val batch = ArrayList<RecordEntity>(READ_BATCH_SIZE)
        var recordsRead = 0
        var lineNo = 0

        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (str in lines) {
                lineNo++
                getRecordEntity(str, "$packageName/$segmentName#$lineNo")?.let { recordEntity ->
                    batch.add(recordEntity)
                    recordsRead++
                    if (batch.size >= READ_BATCH_SIZE) {
                        onBatch(ArrayList(batch))
                        batch.clear()
                    }
                }
            }
        }

        if (batch.isNotEmpty()) {
            onBatch(ArrayList(batch))
        }
        return SegmentReadResult(recordsRead = recordsRead)
    }

    private fun shouldStop(startTime: Long, recordsRead: Int): Boolean {
        return recordsRead >= MAX_RECORDS_PER_RUN ||
            System.currentTimeMillis() - startTime >= MAX_RUN_MILLIS
    }

    private fun getRecordEntity(recordStr: String, sourceKey: String): RecordEntity? {
        val jsonRecord = decodeCompressedRecord(recordStr) ?: return null
        return runCatching {
            Json.decodeFromString<RecordEntity>(jsonRecord).copy(sourceKey = sourceKey)
        }.getOrNull()
    }

    private fun decodeCompressedRecord(compressedRecord: String): String? = runCatching {
        val compressedBytes = GuiseBase64.decode(compressedRecord, GuiseBase64.DEFAULT)
        GZIPInputStream(ByteArrayInputStream(compressedBytes)).bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }.getOrNull()

    private fun String.formatPath(packageName: String): String = String.format(this, packageName)

    private fun String.toUriOrNull(): Uri? = runCatching {
        toUri()
    }.getOrNull()

    private data class SegmentReadResult(
        val recordsRead: Int
    )
}
