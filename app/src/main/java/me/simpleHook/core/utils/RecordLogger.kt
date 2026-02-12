package me.simpleHook.core.utils

import me.simpleHook.core.constant.ConfigConstant
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

object RecordLogger {
    private val logQueue = LinkedBlockingQueue<Pair<String, String>>()
    private val executor = Executors.newSingleThreadExecutor()


    private const val LIMIT_SIZE_KB: Int = 1024 * 100

    init {
        executor.execute {
            while (true) {
                try {
                    val (packageName, content) = logQueue.take()
                    appendWithLimit(packageName, content)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun write(packageName: String, content: String) {
        logQueue.offer(packageName to content)
    }

    private fun appendWithLimit(packageName: String, text: String) {
        val file = getLogFile(packageName) ?: return
        if (!file.exists()) file.createNewFile()


        RandomAccessFile(file, "rw").use { random ->
            val channel = random.channel
            channel.use {
                val lock: FileLock = it.lock()
                try {
                    val sizeKb = file.length() / 1024
                    if (sizeKb >= LIMIT_SIZE_KB) {
                        random.setLength(0L)
                    }
                    random.seek(random.length())
                    random.write("${text}\n".toByteArray())
                } finally {
                    lock.release()
                }
            }
        }
    }


    fun readAndClear(packageName: String): List<String> {
        val file = getLogFile(packageName) ?: return emptyList()
        if (!file.exists()) return emptyList()

        RandomAccessFile(file, "rw").use { random ->
            val channel = random.channel
            channel.use { it ->
                it.lock().use {
                    val length = random.length().toInt()
                    val bytes = ByteArray(length)
                    random.seek(0)
                    random.readFully(bytes)
                    val content = String(bytes, Charsets.UTF_8)
                    random.setLength(0L)
                    return content.lines().filter { it.isNotEmpty() }
                }
            }
        }
    }

    fun clear(packageName: String) {
        val file = getLogFile(packageName) ?: return
        if (!file.exists()) return

        RandomAccessFile(file, "rw").use { random ->
            val channel = random.channel
            channel.use {
                it.lock().use {
                    random.setLength(0L)
                }
            }
        }
    }

    fun delete(packageName: String) {
        val file = getLogFile(packageName) ?: return
        if (!file.exists()) return

        RandomAccessFile(file, "rw").use { random ->
            val channel = random.channel
            channel.use {
                it.lock().use {
                    random.close()
                    file.delete()
                }
            }
        }
    }

    private fun getLogFile(packageName: String): File? {

        return FileUtil.generateFile(ConfigConstant.RECORD_PATH.format(packageName))
    }

}
