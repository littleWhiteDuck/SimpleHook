package me.simpleHook.utils

import me.simpleHook.constant.ConfigConstant
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

object RecordLogger {
    private val logQueue = LinkedBlockingQueue<Pair<String, String>>()
    private val executor = Executors.newSingleThreadExecutor()


    private const val LIMIT_SIZE_KB: Int = 1024 * 8

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
                val lock: FileLock = it.lock() // 独占锁（跨进程安全）
                try {
                    // 检查文件大小（单位 KB）
                    val sizeKb = file.length() / 1024
                    if (sizeKb >= LIMIT_SIZE_KB) {
                        random.setLength(0L) // 清空
                    }

                    // 写入末尾
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
                it.lock().use { // 独占锁
                    val length = random.length().toInt()
                    val bytes = ByteArray(length)
                    random.seek(0)
                    random.readFully(bytes)  // 读取所有内容
                    val content = String(bytes, Charsets.UTF_8)
                    random.setLength(0L)  // 清空文件
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
