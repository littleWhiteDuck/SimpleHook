package me.simpleHook.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.constant.Constant
import java.io.*


object FileUtils {

    fun writeTextToFile(content: String, filePath: String, fileName: String) {
        makeFilePath(filePath, fileName)
        val strFilePath = filePath + fileName
        val strContent = "${content}\r\n"
        try {
            val file = File(strFilePath)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            file.writer().use {
                it.write(strContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isGrant(context: Context): Boolean {
        val permission = ActivityCompat.checkSelfPermission(
            context, "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        return permission == PackageManager.PERMISSION_GRANTED
    }

    private fun changeToUri(path: String): String {
        val paths: Array<String> =
            path.replace("/storage/emulated/0/Android/data".toRegex(), "").split("/").toTypedArray()
        val stringBuilder =
            StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata")
        for (p in paths) {
            if (p.isEmpty()) continue
            stringBuilder.append("%2F").append(p)
        }
        return stringBuilder.toString()
    }

    private fun getDocumentFile(documentFile: DocumentFile, dir: String): DocumentFile? {
        try {
            documentFile.listFiles().forEach {
                if (it.name == dir && it.isDirectory) return it
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun exists(documentFile: DocumentFile, name: String): Boolean {
        return try {
            documentFile.findFile(name)!!.exists()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun writeDocumentFile(
        context: Context, path: String, fileName: String, content: String, mimiType: String
    ) {
        try {
            val paths = path.split("/")
            val dataUri = Uri.parse(Constant.ANDROID_DATA_URI)
            var documentFile = DocumentFile.fromTreeUri(context, dataUri)
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = getDocumentFile(documentFile!!, paths[i])
                    ?: documentFile.createDirectory(paths[i])
            }
            val configFile = if (exists(documentFile!!, fileName)) {
                documentFile.findFile(fileName)
            } else {
                documentFile.createFile(mimiType, fileName)
            }
            if (content.isNotEmpty()) {
                configFile?.uri?.let { alterDocument(context, it, content) }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDocumentDirection(
        context: Context, path: String
    ) {
        try {
            val paths = path.split("/")
            val dataUri = Uri.parse(Constant.ANDROID_DATA_URI)
            var documentFile = DocumentFile.fromTreeUri(context, dataUri)
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = getDocumentFile(documentFile!!, paths[i])
                    ?: documentFile.createDirectory(paths[i])
            }
            documentFile?.let {
                if (it.uri.toString().contains("simpleHook")) {
                    it.delete()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun alterDocument(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rwt")?.use {
                FileOutputStream(it.fileDescriptor).use { output ->
                    output.write(content.toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun verifyStoragePermissions(activity: Activity) {
        try {
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                ), 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeFilePath(filePath: String, fileName: String): File? {
        var file: File? = null
        makeRootDirectory(filePath)
        try {
            file = File(filePath + fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return file
    }

    fun fileIsExists(strFile: String): Boolean {
        try {
            val file = File(strFile)
            if (!file.exists()) {
                return false
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun makeRootDirectory(filePath: String) {
        val file: File?
        try {
            file = File(filePath)
            if (!file.exists()) {
                file.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) file.delete()
    }

    fun writeLogToFile(content: String, filePath: String) {
        val strContent = "${content}\r\n"
        try {
            val file = File(filePath)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            if (file.length() > 10 * 1000 * 1000) {
                file.delete()
            }
            FileWriter(file.path, true).use {
                it.write(strContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}