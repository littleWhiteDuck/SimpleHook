package me.simpleHook.compat

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.App
import me.simpleHook.constant.Constant
import me.simpleHook.util.DocumentRUtils
import me.simpleHook.util.DocumentTUtils
import me.simpleHook.util.OSUtils
import java.io.FileOutputStream

object DocumentCompat {

    fun generateAppUri(packageName: String): Uri {
        return Uri.parse(Constant.ANDROID_DATA_URI + "%2F" + packageName)
    }

    fun generateFileUri(packageName: String, filePath: String): Uri {
        return if (OSUtils.atLeastT()) {
            DocumentTUtils.generateFileUri(packageName, filePath)
        } else {
            DocumentRUtils.generateFileUri(packageName, filePath)
        }
    }


    fun changeToUri(path: String): String {
        val paths: Array<String> =
            path.replace("/storage/emulated/0/Android/data".toRegex(), "").split("/").toTypedArray()
        val stringBuilder = StringBuilder(Constant.ANDROID_DATA_URI)
        for (p in paths) {
            if (p.isEmpty()) continue
            stringBuilder.append("%2F").append(p)
        }
        return stringBuilder.toString()
    }

    fun getDocumentFile(documentFile: DocumentFile, dir: String): DocumentFile? {
        try {
            documentFile.listFiles().forEach {
                if (it.name == dir && it.isDirectory) return it
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    fun deleteFile(packageName: String, filePath: String): Boolean {
        return runCatching {
            val uri = generateFileUri(packageName, filePath)
            if (isFileExists(App, uri)) {
                DocumentsContract.deleteDocument(App.contentResolver, uri)
            }
            true
        }.getOrDefault(false)
    }

    fun isChildExists(documentFile: DocumentFile, name: String): Boolean {
        return runCatching {
            documentFile.findFile(name)?.exists() ?: false
        }.getOrDefault(false)
    }

    fun isFileExists(context: Context, documentFileUri: Uri): Boolean {
        return DocumentFile.fromSingleUri(context, documentFileUri)?.exists() ?: false
    }

    fun makeDirs(context: Context, path: String, packageName: String): Boolean {
        return if (OSUtils.atLeastT()) {
            DocumentTUtils.makeDirs(context, path, packageName)
        } else {
            DocumentRUtils.makeDirs(context, path)
        }
    }

    fun outTextToFile(
        context: Context,
        packageName: String,
        fileName: String,
        content: String,
        mimiType: String = "application/json"
    ): Boolean {
        return if (OSUtils.atLeastT()) {
            DocumentTUtils.outTextToFile(context, packageName, fileName, content, mimiType)
        } else {
            DocumentRUtils.outTextToFile(context, packageName, fileName, content, mimiType)
        }
    }


    fun deleteDocumentDir(
        context: Context, path: String
    ): Boolean {
        return runCatching {
            val paths = path.replace(Constant.ANDROID_DATA_PATH, "").split("/")
            val dataUri = Uri.parse(Constant.ANDROID_DATA_URI)
            var documentFile = DocumentFile.fromTreeUri(context, dataUri)
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = getDocumentFile(documentFile!!, paths[i])
            }
            documentFile?.delete() ?: false
        }.getOrDefault(false)
    }

    fun alterDocument(context: Context, uri: Uri, content: String): Boolean {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "rwt")?.use {
                FileOutputStream(it.fileDescriptor).use { output ->
                    output.write(content.toByteArray())
                }
            }
            true
        }.getOrDefault(false)
    }

    fun getFileUri(context: Context, packageName: String, path: String): Uri? {
        return if (OSUtils.atLeastT()) {
            DocumentTUtils.getFileUri(context, packageName, path)
        } else {
            DocumentRUtils.getFileUri(context, packageName, path)
        }
    }


}