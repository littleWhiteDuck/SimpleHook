package me.simpleHook.util

import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.topjohnwu.superuser.nio.ExtendedFile
import java.io.IOException

object SuFileUtils {


    fun deleteFile(filePath: String): Boolean {
        val suFile = SuFile.open(filePath)
        if (!suFile.exists()) return true
        return if (suFile.isDirectory) {
            deleteDir(suFile)
        } else {
            suFile.delete()
        }
    }

    fun deleteDir(dirPath: String): Boolean {
        return deleteDir(SuFile(dirPath))
    }

    fun deleteDir(dir: ExtendedFile): Boolean {
        if (!dir.isDirectory) return false
        val files = dir.listFiles() ?: return false
        for (file in files) {
            if (!deleteFile(file.path)) return false
        }
        return dir.delete()
    }

    fun makeDirs(pathFile: String): Boolean {
        return makeDirs(SuFile.open(pathFile))
    }

    fun makeDirs(dir: ExtendedFile): Boolean {
        return if (dir.exists()) {
            true
        } else {
            dir.mkdirs()
        }
    }

    fun isFileExists(filePath: String): Boolean {
        return SuFile(filePath).exists()
    }

    fun generateFile(filePath: String): ExtendedFile? {
        val file = SuFile.open(filePath)
        return if (file.exists()) {
            file
        } else {
            try {
                file.parentFile?.let { makeDirs(it.path) }
                if (file.createNewFile()) file else null
            } catch (e: IOException) {
                null
            }
        }
    }

    fun outTextToFile(pathName: String, content: String): Boolean {
        return runCatching {
            generateFile(pathName)?.let {
                SuFileOutputStream.open(it).writer().use { out ->
                    out.write(content)
                }
                SuUtil.chmodConfigFile(pathName)
                true
            } ?: false
        }.getOrDefault(false)
    }
}