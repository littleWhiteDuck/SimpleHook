package me.simpleHook.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.ExtRecordSettings
import java.io.File
import java.util.Locale
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
    private const val HOST_MAX_SEGMENTS = 64
    private const val MIN_CACHE_MB = 64
    private const val MAX_CACHE_MB = 256
    private const val STALE_TMP_AGE_MS = 10L * 60L * 1000L

    private val logQueue = LinkedBlockingDeque<LogItem>(MAX_QUEUE_RECORDS)
    private val executor = Executors.newSingleThreadExecutor()
    private val segmentSeq = AtomicLong()
    private val json = Json { encodeDefaults = true }

    private val droppedInMemoryByPackage = ConcurrentHashMap<String, AtomicLong>()
    private val maxCacheMbByPackage = ConcurrentHashMap<String, Int>()

    init {
        executor.execute {
            val buffers = mutableMapOf<String, PackageBuffer>()
            var lastFlush = System.currentTimeMillis()
            while (true) {
                try {
                    val item = logQueue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS)
                    if (item != null) {
                        val buffer = buffers.getOrPut(item.packageName) { PackageBuffer() }
                        buffer.add(item.content)
                        if (buffer.shouldFlush()) {
                            flushPackage(item.packageName, buffer)
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

    fun write(packageName: String, content: String) {
        if (!logQueue.offer(LogItem(packageName, content))) {
            val dropped = logQueue.pollFirst()
            droppedInMemoryByPackage
                .computeIfAbsent(dropped?.packageName ?: packageName) { AtomicLong() }
                .incrementAndGet()
            logQueue.offer(LogItem(packageName, content))
        }
    }

    fun applyRecordSettings(packageName: String, settings: ExtRecordSettings?) {
        val maxCacheMb = (settings?.maxCacheMb ?: MIN_CACHE_MB)
            .coerceIn(MIN_CACHE_MB, MAX_CACHE_MB)
        maxCacheMbByPackage[packageName] = maxCacheMb
    }

    fun clear(packageName: String) {
        File(ConfigConstant.RECORD_QUEUE_DIR.formatPath(packageName)).deleteRecursively()
    }

    fun delete(packageName: String) {
        clear(packageName)
    }

    private fun flushPackage(packageName: String, buffer: PackageBuffer) {
        if (buffer.isEmpty()) return

        val tmpDir = File(ConfigConstant.RECORD_QUEUE_TMP_DIR.formatPath(packageName))
        val readyDir = File(ConfigConstant.RECORD_QUEUE_READY_DIR.formatPath(packageName))
        if (!tmpDir.exists()) tmpDir.mkdirs()
        if (!readyDir.exists()) readyDir.mkdirs()
        cleanupStaleTmpFiles(tmpDir)

        val seq = segmentSeq.incrementAndGet()
        val baseName = buildSegmentName(seq)
        val tmpFile = File(tmpDir, "$baseName.tmp")
        val readyFile = File(readyDir, "$baseName.seg")
        val records = buffer.drain()
        val bytes = records.sumOf { it.toByteArray(Charsets.UTF_8).size + 1 }

        runCatching {
            tmpFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                records.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
            if (!tmpFile.renameTo(readyFile)) {
                tmpFile.copyTo(readyFile, overwrite = true)
                tmpFile.delete()
            }
            enforceQuota(packageName, readyDir)
            writeStats(packageName, readyDir, bytesWritten = bytes.toLong(), recordsWritten = records.size)
        }.onFailure {
            tmpFile.delete()
            it.printStackTrace()
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

    private fun enforceQuota(packageName: String, readyDir: File) {
        val segments = listSegments(readyDir).toMutableList()
        val hostMaxBytes = maxCacheBytes(packageName)
        var totalBytes = segments.sumOf { it.length() }
        var droppedSegments = 0L
        var droppedBytes = 0L

        while (segments.size > HOST_MAX_SEGMENTS || totalBytes > hostMaxBytes) {
            val oldest = segments.removeFirstOrNull() ?: break
            val len = oldest.length()
            if (oldest.delete()) {
                totalBytes -= len
                droppedSegments++
                droppedBytes += len
            } else {
                break
            }
        }

        if (droppedSegments > 0 || droppedBytes > 0) {
            writeStats(
                packageName = packageName,
                readyDir = readyDir,
                droppedOnDisk = droppedSegments,
                droppedBytes = droppedBytes
            )
        }
    }

    private fun writeStats(
        packageName: String,
        readyDir: File,
        bytesWritten: Long = 0L,
        recordsWritten: Int = 0,
        droppedOnDisk: Long = 0L,
        droppedBytes: Long = 0L
    ) {
        val segments = listSegments(readyDir)
        val statsFile = File(ConfigConstant.RECORD_QUEUE_STATS_PATH.formatPath(packageName))
        val oldStats = readStats(statsFile)
        val memoryDropCount = droppedInMemoryByPackage[packageName]?.get() ?: 0L
        val newStats = oldStats.copy(
            droppedInMemory = memoryDropCount,
            droppedOnDisk = oldStats.droppedOnDisk + droppedOnDisk,
            droppedBytes = oldStats.droppedBytes + droppedBytes,
            readyBytes = segments.sumOf { it.length() },
            readySegments = segments.size,
            writtenBytes = oldStats.writtenBytes + bytesWritten,
            writtenRecords = oldStats.writtenRecords + recordsWritten,
            lastDropTime = if (droppedOnDisk > 0 || droppedBytes > 0 || memoryDropCount > oldStats.droppedInMemory) {
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

    private fun buildSegmentName(seq: Long): String {
        val timestamp = TimeUtil.getCurrentTime("yyyyMMdd_HHmmss_SSS")
        val pid = android.os.Process.myPid()
        return "%s_%d_%06d".format(Locale.US, timestamp, pid, seq)
    }

    private fun maxCacheBytes(packageName: String): Long {
        val maxCacheMb = maxCacheMbByPackage[packageName] ?: MIN_CACHE_MB
        return maxCacheMb.toLong() * 1024L * 1024L
    }

    private fun String.formatPath(packageName: String): String = String.format(this, packageName)

    private data class LogItem(
        val packageName: String,
        val content: String
    )

    private class PackageBuffer {
        private val lines = ArrayList<String>()
        private var bytes = 0

        fun add(content: String) {
            lines.add(content)
            bytes += content.toByteArray(Charsets.UTF_8).size + 1
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
        val droppedOnDisk: Long = 0,
        val droppedBytes: Long = 0,
        val readyBytes: Long = 0,
        val readySegments: Int = 0,
        val writtenBytes: Long = 0,
        val writtenRecords: Int = 0,
        val lastDropTime: String = ""
    )
}
