package me.simpleHook.core.utils

import java.io.File
import java.io.FileWriter
import java.io.IOException


object FileUtil {

    fun deleteFile(filePath: String): Boolean {
        return deleteFile(File(filePath))
    }

    fun deleteFile(file: File?): Boolean {
        file ?: return false
        if (!file.exists()) return true
        return if (file.isDirectory) {
            deleteDir(file)
        } else {
            file.delete()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        dir ?: return false
        if (!dir.isDirectory) return false
        val files = dir.listFiles() ?: return false
        for (file in files) {
            if (!deleteFile(file)) return false
        }
        return dir.delete()
    }

    fun makeDirs(pathFile: String): Boolean {
        return makeDirs(File(pathFile))
    }

    fun makeDirs(dir: File?): Boolean {
        dir ?: return false
        return if (dir.exists()) {
            true
        } else {
            dir.mkdirs()
        }
    }

    fun isFileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    fun isFileExists(parentPath: String, childName: String): Boolean {
        return File(parentPath, childName).exists()
    }

    fun generateFile(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists()) {
            if (file.isDirectory) {
                file.delete()
                generateFile(filePath)
            } else {
                file
            }
        } else {
            try {
                makeDirs(file.parentFile)
                if (file.createNewFile()) file else null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun copyFile(srcPath: String, desPath: String): Boolean {
        val srcFile = File(srcPath)
        if (!srcFile.exists() || !srcFile.isFile) {
            return false
        }

        return try {
            val desFile = File(desPath)
            desFile.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
            }
            desFile.outputStream().use { outputStream ->
                srcFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun outTextToFile(
        filePath: String,
        content: String,
        isNewLine: Boolean = false,
        limitSize: Int = -1,
        append: Boolean = false
    ): Boolean {
        var file = generateFile(filePath) ?: return false
        if (limitSize != -1) {
            if (file.length() > limitSize * 1000) {
                file.delete()
                file = generateFile(filePath) ?: return false
            }
        }
        FileWriter(file, append).use {
            it.write(content)
            if (isNewLine) it.write("\r\n")
        }
        return true
    }
}