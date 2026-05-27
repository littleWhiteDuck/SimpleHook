package me.simpleHook.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.ExtRecordSettings
import me.simpleHook.data.record.RecordSource
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object RecordLogger {
    private const val MAX_QUEUE_RECORDS = 4096
    private const val SEGMENT_MAX_BYTES = 512 * 1024
    private const val MAX_RECORDS_PER_SEGMENT = 1000
    private const val FLUSH_INTERVAL_MS = 1000L
    private const val CUSTOM_CACHE_MB = 32
    private const val CUSTOM_MAX_RECORD_MB = 4
    private const val EXTENSION_MIN_CACHE_MB = 64
    private const val EXTENSION_MAX_CACHE_MB = 256
    private const val EXTENSION_DEFAULT_CACHE_MB = 64
    private const val EXTENSION_MIN_RECORD_MB = 1
    private const val EXTENSION_MAX_RECORD_MB = 16
    private const val EXTENSION_DEFAULT_RECORD_MB = 4
    private const val MIN_SEGMENTS_PER_SOURCE = 16
    private const val MAX_SEGMENTS_PER_SOURCE = 512
    private const val STALE_TMP_AGE_MS = 10L * 60L * 1000L
    private const val MANIFEST_NAME = "manifest.txt"

    private val logQueue = LinkedBlockingDeque<LogItem>(MAX_QUEUE_RECORDS)
    private val executor = Executors.newSingleThreadExecutor()
    private val segmentSeq = AtomicLong()
    private val json = Json { encodeDefaults = true }

    private val droppedInMemoryBySource = ConcurrentHashMap<BufferKey, AtomicLong>()
    private val droppedOversizedBySource = ConcurrentHashMap<BufferKey, AtomicLong>()
    private val droppedOversizedBytesBySource = ConcurrentHashMap<BufferKey, AtomicLong>()
    private val extensionMaxCacheMbByPackage = ConcurrentHashMap<String, Int>()
    private val extensionMaxRecordMbByPackage = ConcurrentHashMap<String, Int>()

    init {
        executor.execute {
            val buffers = mutableMapOf<BufferKey, PackageBuffer>()
            var lastFlush = System.currentTimeMillis()
            while (true) {
                try {
                    val item = logQueue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS)
                    if (item != null) {
                        val key = BufferKey(item.packageName, item.source)
                        val buffer = buffers.getOrPut(key) { PackageBuffer() }
                        buffer.add(item.content)
                        if (buffer.shouldFlush()) {
                            flushPackage(key, buffer)
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastFlush >= FLUSH_INTERVAL_MS) {
                        val iterator = buffers.iterator()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            flushPackage(entry.key, entry.value)
                            if (entry.value.isEmpty()) {
                                iterator.remove()
                            }
                        }
                        lastFlush = now
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun write(packageName: String, content: String, source: RecordSource) {
        if (shouldDropForRecordSize(packageName, source, content.length)) return

        val item = LogItem(packageName, content, source)
        if (logQueue.offer(item)) return

        val dropped = logQueue.pollFirst()
        val droppedKey = dropped?.let { BufferKey(it.packageName, it.source) }
            ?: BufferKey(packageName, source)
        droppedInMemoryBySource
            .computeIfAbsent(droppedKey) { AtomicLong() }
            .incrementAndGet()

        if (!logQueue.offer(item)) {
            droppedInMemoryBySource
                .computeIfAbsent(BufferKey(packageName, source)) { AtomicLong() }
                .incrementAndGet()
        }
    }

    fun shouldDropForRecordSize(
        packageName: String,
        source: RecordSource,
        recordSize: Int
    ): Boolean {
        val key = BufferKey(packageName, source)
        if (recordSize.toLong() <= maxRecordBytes(key)) return false
        recordOversizedDrop(key, recordSize.toLong())
        return true
    }

    fun applyRecordSettings(packageName: String, settings: ExtRecordSettings?) {
        val maxCacheMb = (settings?.maxCacheMb ?: EXTENSION_DEFAULT_CACHE_MB)
            .coerceIn(EXTENSION_MIN_CACHE_MB, EXTENSION_MAX_CACHE_MB)
        val maxRecordMb = (settings?.maxRecordMb ?: EXTENSION_DEFAULT_RECORD_MB)
            .coerceIn(EXTENSION_MIN_RECORD_MB, EXTENSION_MAX_RECORD_MB)
        extensionMaxCacheMbByPackage[packageName] = maxCacheMb
        extensionMaxRecordMbByPackage[packageName] = maxRecordMb
    }

    fun clear(packageName: String) {
        File(ConfigConstant.RECORD_DIR.formatPath(packageName)).deleteRecursively()
    }

    fun delete(packageName: String) {
        clear(packageName)
    }

    private fun flushPackage(key: BufferKey, buffer: PackageBuffer) {
        if (buffer.isEmpty()) return

        val sourceDir = File(ConfigConstant.RECORD_SOURCE_DIR.formatPath(key.packageName, key.source.dirName))
        val tmpDir = File(ConfigConstant.RECORD_SOURCE_TMP_DIR.formatPath(key.packageName, key.source.dirName))
        val readyDir = File(ConfigConstant.RECORD_SOURCE_READY_DIR.formatPath(key.packageName, key.source.dirName))
        if (!sourceDir.exists()) sourceDir.mkdirs()
        if (!tmpDir.exists()) tmpDir.mkdirs()
        if (!readyDir.exists()) readyDir.mkdirs()
        cleanupStaleTmpFiles(tmpDir)

        val seq = segmentSeq.incrementAndGet()
        val baseName = buildSegmentName(seq)
        val tmpFile = File(tmpDir, "$baseName.tmp")
        val readyFile = File(readyDir, "$baseName.seg")
        val records = buffer.drain()
        val bytes = records.sumOf { it.length + 1 }

        runCatching {
            tmpFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                records.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
            withSourceLock(sourceDir) {
                if (!tmpFile.renameTo(readyFile)) {
                    tmpFile.copyTo(readyFile, overwrite = true)
                    tmpFile.delete()
                }
                enforceQuota(key, readyDir)
                rewriteManifest(sourceDir, readyDir)
                writeStats(key, readyDir, bytesWritten = bytes.toLong(), recordsWritten = records.size)
            }
        }.onFailure {
            tmpFile.delete()
            it.printStackTrace()
        }
    }

    private fun withSourceLock(sourceDir: File, block: () -> Unit) {
        if (!sourceDir.exists()) sourceDir.mkdirs()
        val lockFile = File(sourceDir, ".record.lock")
        RandomAccessFile(lockFile, "rw").use { raf ->
            raf.channel.use { channel ->
                val lock = channel.lock()
                try {
                    block()
                } finally {
                    lock.release()
                }
            }
        }
    }

    private fun cleanupStaleTmpFiles(tmpDir: File) {
        val now = System.currentTimeMillis()
        tmpDir.listFiles { file -> file.isFile && file.extension == "tmp" }?.forEach { file ->
            if (now - file.lastModified() > STALE_TMP_AGE_MS) {
                file.delete()
            }
        }
    }

    private fun enforceQuota(key: BufferKey, readyDir: File) {
        val segments = listSegments(readyDir).toMutableList()
        val maxBytes = maxCacheBytes(key)
        val maxSegments = maxSegmentCount(maxBytes)
        var totalBytes = segments.sumOf { it.length() }
        var droppedSegments = 0L
        var droppedBytes = 0L

        while ((segments.size > maxSegments || totalBytes > maxBytes) && segments.size > 1) {
            val oldest = segments.removeFirstOrNull() ?: break
            val len = oldest.length()
            if (!oldest.exists() || oldest.delete()) {
                totalBytes -= len
                droppedSegments++
                droppedBytes += len
            } else {
                break
            }
        }

        if (droppedSegments > 0 || droppedBytes > 0) {
            writeStats(
                key = key,
                readyDir = readyDir,
                droppedOnDisk = droppedSegments,
                droppedBytes = droppedBytes
            )
        }
    }

    private fun writeStats(
        key: BufferKey,
        readyDir: File,
        bytesWritten: Long = 0L,
        recordsWritten: Int = 0,
        droppedOnDisk: Long = 0L,
        droppedBytes: Long = 0L
    ) {
        val segments = listSegments(readyDir)
        val statsFile = File(ConfigConstant.RECORD_SOURCE_STATS_PATH.formatPath(key.packageName, key.source.dirName))
        val oldStats = readStats(statsFile)
        val memoryDropCount = droppedInMemoryBySource[key]?.get() ?: 0L
        val oversizedDropCount = droppedOversizedBySource[key]?.get() ?: 0L
        val oversizedDropBytes = droppedOversizedBytesBySource[key]?.get() ?: 0L
        val newStats = oldStats.copy(
            droppedInMemory = memoryDropCount,
            droppedOversized = oversizedDropCount,
            droppedOnDisk = oldStats.droppedOnDisk + droppedOnDisk,
            droppedBytes = oldStats.droppedBytes + droppedBytes,
            droppedOversizedBytes = oversizedDropBytes,
            readyBytes = segments.sumOf { it.length() },
            readySegments = segments.size,
            writtenBytes = oldStats.writtenBytes + bytesWritten,
            writtenRecords = oldStats.writtenRecords + recordsWritten,
            lastDropTime = if (
                droppedOnDisk > 0 ||
                droppedBytes > 0 ||
                memoryDropCount > oldStats.droppedInMemory ||
                oversizedDropCount > oldStats.droppedOversized
            ) {
                TimeUtil.getCurrentTime("yy-MM-dd HH:mm:ss")
            } else {
                oldStats.lastDropTime
            }
        )
        FileUtil.generateFile(statsFile.path)?.writeText(json.encodeToString(newStats))
    }

    private fun readStats(statsFile: File): RecordQueueStats {
        if (!statsFile.canRead()) return RecordQueueStats()
        return runCatching {
            json.decodeFromString<RecordQueueStats>(statsFile.readText())
        }.getOrDefault(RecordQueueStats())
    }

    private fun listSegments(readyDir: File): List<File> {
        return readyDir.listFiles { file -> file.isFile && file.extension == "seg" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun rewriteManifest(sourceDir: File, readyDir: File) {
        val segmentNames = listSegments(readyDir).map { it.name }
        val manifestFile = File(sourceDir, MANIFEST_NAME)
        val tmpFile = File(sourceDir, "$MANIFEST_NAME.tmp")
        val content = if (segmentNames.isEmpty()) {
            ""
        } else {
            segmentNames.joinToString(separator = "\n", postfix = "\n")
        }
        FileUtil.generateFile(tmpFile.path)?.writeText(content) ?: return
        if (!tmpFile.renameTo(manifestFile)) {
            manifestFile.delete()
            if (!tmpFile.renameTo(manifestFile)) {
                tmpFile.copyTo(manifestFile, overwrite = true)
                tmpFile.delete()
            }
        }
    }

    private fun buildSegmentName(seq: Long): String {
        val timestamp = TimeUtil.getCurrentTime("yyyyMMdd_HHmmss_SSS")
        val pid = android.os.Process.myPid()
        val nonce = UUID.randomUUID().toString().replace("-", "").take(8)
        return "%s_%d_%s_%06d".format(Locale.US, timestamp, pid, nonce, seq)
    }

    private fun maxCacheBytes(key: BufferKey): Long {
        val maxCacheMb = when (key.source) {
            RecordSource.Custom -> CUSTOM_CACHE_MB
            RecordSource.Extension -> extensionMaxCacheMbByPackage[key.packageName] ?: EXTENSION_DEFAULT_CACHE_MB
        }
        return maxCacheMb.toLong() * 1024L * 1024L
    }

    private fun maxRecordBytes(key: BufferKey): Long {
        val maxRecordMb = when (key.source) {
            RecordSource.Custom -> CUSTOM_MAX_RECORD_MB
            RecordSource.Extension -> extensionMaxRecordMbByPackage[key.packageName]
                ?: EXTENSION_DEFAULT_RECORD_MB
        }
        return maxRecordMb.toLong() * 1024L * 1024L
    }

    private fun maxSegmentCount(maxBytes: Long): Int {
        return (maxBytes / SEGMENT_MAX_BYTES)
            .coerceAtLeast(MIN_SEGMENTS_PER_SOURCE.toLong())
            .coerceAtMost(MAX_SEGMENTS_PER_SOURCE.toLong())
            .toInt()
    }

    private fun recordOversizedDrop(key: BufferKey, bytes: Long) {
        droppedOversizedBySource
            .computeIfAbsent(key) { AtomicLong() }
            .incrementAndGet()
        droppedOversizedBytesBySource
            .computeIfAbsent(key) { AtomicLong() }
            .addAndGet(bytes)
    }

    private fun String.formatPath(vararg args: Any): String = String.format(this, *args)

    private data class LogItem(
        val packageName: String,
        val content: String,
        val source: RecordSource
    )

    private data class BufferKey(
        val packageName: String,
        val source: RecordSource
    )

    private class PackageBuffer {
        private val lines = ArrayList<String>()
        private var bytes = 0

        fun add(content: String) {
            lines.add(content)
            bytes += content.length + 1
        }

        fun shouldFlush(): Boolean {
            return bytes >= SEGMENT_MAX_BYTES || lines.size >= MAX_RECORDS_PER_SEGMENT
        }

        fun drain(): List<String> {
            val snapshot = ArrayList(lines)
            lines.clear()
            bytes = 0
            return snapshot
        }

        fun isEmpty(): Boolean = lines.isEmpty()
    }

    @Serializable
    data class RecordQueueStats(
        val droppedInMemory: Long = 0,
        val droppedOversized: Long = 0,
        val droppedOnDisk: Long = 0,
        val droppedBytes: Long = 0,
        val droppedOversizedBytes: Long = 0,
        val readyBytes: Long = 0,
        val readySegments: Int = 0,
        val writtenBytes: Long = 0,
        val writtenRecords: Int = 0,
        val lastDropTime: String = ""
    )
}
