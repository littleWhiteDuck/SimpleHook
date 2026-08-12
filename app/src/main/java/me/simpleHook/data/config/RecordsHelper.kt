package me.simpleHook.data.config

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
import me.simpleHook.data.record.RecordSource
import me.simpleHook.platform.shizuku.ShizukuFileManager
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream

object RecordsHelper {
    private const val READ_BATCH_SIZE = 200
    private const val MAX_RECORDS_PER_RUN = 2000
    private const val MAX_SEGMENTS_PER_RUN = 8
    private const val MAX_RUN_MILLIS = 1500L
    private const val CUSTOM_MAX_COMPRESSED_RECORD_BYTES = 4 * 1024 * 1024
    private const val EXTENSION_MAX_COMPRESSED_RECORD_BYTES = 16 * 1024 * 1024
    private const val LOCK_WAIT_MILLIS = 1000L
    private const val LOCK_POLL_MILLIS = 100L
    private const val CANCEL_CHECK_INTERVAL_LINES = 64

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
        val recordPath = ConfigConstant.RECORD_DIR.formatPath(packageName)
        val lock = getLock(recordPath)
        val budget = ReadBudget()
        val coroutineContext = currentCoroutineContext()
        if (!lock.tryLockCancellable()) return
        try {
            for (source in RecordSource.values()) {
                coroutineContext.ensureActive()
                if (shouldStop(budget)) break
                val readyPath = ConfigConstant.RECORD_SOURCE_READY_DIR.formatPath(
                    packageName,
                    source.dirName
                )
                if (GlobalValue.isRootWork) {
                    rootReadSegments(packageName, source, readyPath, budget, onBatch)
                } else if (GlobalValue.isShizukuWork) {
                    shizukuReadSegments(packageName, source, readyPath, budget, onBatch)
                } else {
                    fileReadSegments(packageName, source, readyPath, context, budget, onBatch)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtil.outLog(e.stackTraceToString())
        } finally {
            lock.unlock()
        }
    }

    private suspend fun shizukuReadSegments(
        packageName: String,
        source: RecordSource,
        readyPath: String,
        budget: ReadBudget,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val service = ShizukuFileManager.service ?: return
        val cacheRoot = App.externalCacheDir ?: App.cacheDir
        val cacheDir = cacheRoot.resolve("record_segments").also { it.mkdirs() }
        val names = shizukuReadManifestNames(packageName, source, cacheDir)
        if (names.isEmpty()) return

        val coroutineContext = currentCoroutineContext()
        for (name in names) {
            coroutineContext.ensureActive()
            if (shouldStop(budget)) break
            val localFile = File(cacheDir, "${source.dirName}_$name")
            val remotePath = "$readyPath/$name"
            if (service.copyFile(remotePath, localFile.path) && localFile.canRead()) {
                val result = localFile.inputStream().use { input ->
                    emitRecordBatches(
                        input = input,
                        packageName = packageName,
                        source = source,
                        segmentName = name,
                        onBatch = onBatch
                    )
                }
                budget.consume(result)
                localFile.delete()
                service.deleteFile(remotePath)
            }
        }
    }

    private suspend fun rootReadSegments(
        packageName: String,
        source: RecordSource,
        readyPath: String,
        budget: ReadBudget,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val names = rootReadManifestNames(packageName, source)
        if (names.isEmpty()) return

        val coroutineContext = currentCoroutineContext()
        for (name in names) {
            coroutineContext.ensureActive()
            if (shouldStop(budget)) break
            val segment = SuFile.open("$readyPath/$name")
            if (!segment.exists() || !segment.isFile) continue
            val result = SuFileInputStream.open(segment).use { input ->
                emitRecordBatches(
                    input = input,
                    packageName = packageName,
                    source = source,
                    segmentName = name,
                    onBatch = onBatch
                )
            }
            budget.consume(result)
            segment.delete()
        }
    }

    private fun shizukuReadManifestNames(
        packageName: String,
        source: RecordSource,
        cacheDir: File
    ): List<String> {
        val service = ShizukuFileManager.service ?: return emptyList()
        val remotePath = ConfigConstant.RECORD_SOURCE_MANIFEST_PATH.formatPath(
            packageName,
            source.dirName
        )
        val localFile = File(cacheDir, "manifest_${packageName.hashCode()}_${source.dirName}.txt")
        if (!service.copyFile(remotePath, localFile.path) || !localFile.canRead()) {
            localFile.delete()
            return emptyList()
        }
        val names = localFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            normalizeSegmentNames(lines)
        }
        localFile.delete()
        return names
    }

    private fun rootReadManifestNames(
        packageName: String,
        source: RecordSource
    ): List<String> {
        val manifestPath = ConfigConstant.RECORD_SOURCE_MANIFEST_PATH.formatPath(
            packageName,
            source.dirName
        )
        val manifestFile = SuFile.open(manifestPath)
        if (!manifestFile.exists() || !manifestFile.isFile) return emptyList()
        return SuFileInputStream.open(manifestFile).bufferedReader(Charsets.UTF_8).useLines { lines ->
            normalizeSegmentNames(lines)
        }
    }

    private fun normalizeSegmentNames(lines: Sequence<String>): List<String> {
        return lines
            .map { it.trim() }
            .filter(::isSegmentName)
            .distinct()
            .toList()
    }

    private fun isSegmentName(name: String): Boolean {
        return name.isNotEmpty() &&
            name.endsWith(".seg") &&
            !name.contains("/") &&
            !name.contains("\\")
    }

    private suspend fun fileReadSegments(
        packageName: String,
        source: RecordSource,
        readyPath: String,
        context: Context,
        budget: ReadBudget,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        if (OSUtil.atLeastR()) {
            documentReadSegments(packageName, source, readyPath, context, budget, onBatch)
        } else {
            localFileReadSegments(packageName, source, readyPath, budget, onBatch)
        }
    }

    private suspend fun documentReadSegments(
        packageName: String,
        source: RecordSource,
        readyPath: String,
        context: Context,
        budget: ReadBudget,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val rootUri = Constant.ANDROID_DATA_URI.toUriOrNull() ?: return
        val childPath = readyPath.removePrefix(Constant.ANDROID_DATA_PATH)
        val readyDir = DocumentCompat.getDocumentFile(context, rootUri, childPath) ?: return
        val segments = readyDir.listFiles()
            .filter { it.isFile && it.name?.endsWith(".seg") == true }
            .sortedBy { it.name.orEmpty() }

        val coroutineContext = currentCoroutineContext()
        for (segment in segments) {
            coroutineContext.ensureActive()
            if (shouldStop(budget)) break
            val name = segment.name ?: continue
            val input = context.contentResolver.openInputStream(segment.uri) ?: continue
            val result = input.use {
                emitRecordBatches(
                    input = it,
                    packageName = packageName,
                    source = source,
                    segmentName = name,
                    onBatch = onBatch
                )
            }
            budget.consume(result)
            DocumentsContract.deleteDocument(context.contentResolver, segment.uri)
        }
    }

    private suspend fun localFileReadSegments(
        packageName: String,
        source: RecordSource,
        readyPath: String,
        budget: ReadBudget,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val segments = File(readyPath).listFiles { file -> file.isFile && file.extension == "seg" }
            ?.sortedBy { it.name }
            ?: return

        val coroutineContext = currentCoroutineContext()
        for (segment in segments) {
            coroutineContext.ensureActive()
            if (shouldStop(budget)) break
            val result = segment.inputStream().use { input ->
                emitRecordBatches(
                    input = input,
                    packageName = packageName,
                    source = source,
                    segmentName = segment.name,
                    onBatch = onBatch
                )
            }
            budget.consume(result)
            FileUtil.deleteFile(segment)
        }
    }

    private suspend fun emitRecordBatches(
        input: InputStream,
        packageName: String,
        source: RecordSource,
        segmentName: String,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): SegmentReadResult {
        val batch = ArrayList<RecordEntity>(READ_BATCH_SIZE)
        val coroutineContext = currentCoroutineContext()
        var recordsRead = 0
        var lineNo = 0

        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (str in lines) {
                lineNo++
                if (lineNo % CANCEL_CHECK_INTERVAL_LINES == 0) {
                    coroutineContext.ensureActive()
                }
                if (isOversizedRecordLine(source, str)) {
                    continue
                }
                val sourceKey = "$packageName/${source.dirName}/$segmentName#$lineNo"
                getRecordEntity(str, sourceKey)?.let { recordEntity ->
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

    private fun isOversizedRecordLine(source: RecordSource, recordLine: String): Boolean {
        val maxBytes = when (source) {
            RecordSource.Custom -> CUSTOM_MAX_COMPRESSED_RECORD_BYTES
            RecordSource.Extension -> EXTENSION_MAX_COMPRESSED_RECORD_BYTES
        }
        return recordLine.length > maxBytes
    }

    private fun shouldStop(budget: ReadBudget): Boolean {
        return budget.recordsRead >= MAX_RECORDS_PER_RUN ||
            budget.segmentsRead >= MAX_SEGMENTS_PER_RUN ||
            System.currentTimeMillis() - budget.startTime >= MAX_RUN_MILLIS
    }

    private suspend fun ReentrantLock.tryLockCancellable(): Boolean {
        val coroutineContext = currentCoroutineContext()
        val deadline = System.currentTimeMillis() + LOCK_WAIT_MILLIS
        while (true) {
            coroutineContext.ensureActive()
            if (tryLock(LOCK_POLL_MILLIS, TimeUnit.MILLISECONDS)) return true
            if (System.currentTimeMillis() >= deadline) return false
        }
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

    private fun String.formatPath(vararg args: Any): String = String.format(this, *args)

    private fun String.toUriOrNull(): Uri? = runCatching {
        toUri()
    }.getOrNull()

    private data class SegmentReadResult(
        val recordsRead: Int
    )

    private data class ReadBudget(
        val startTime: Long = System.currentTimeMillis(),
        var recordsRead: Int = 0,
        var segmentsRead: Int = 0
    ) {
        fun consume(result: SegmentReadResult) {
            recordsRead += result.recordsRead
            segmentsRead++
        }
    }
}
