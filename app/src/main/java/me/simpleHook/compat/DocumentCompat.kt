@file:Suppress("unused")

package me.simpleHook.compat

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.App
import me.simpleHook.constant.Constant
import me.simpleHook.utils.DocumentRUtil
import me.simpleHook.utils.DocumentTUtil
import me.simpleHook.utils.OSUtil
import java.io.FileOutputStream

object DocumentCompat {

    fun generateAppUri(packageName: String): Uri {
        return (Constant.ANDROID_DATA_URI + "%2F" + packageName).toUri()
    }

    fun generateFileUri(packageName: String, filePath: String): Uri {
        return if (OSUtil.atLeastT()) {
            DocumentTUtil.generateFileUri(packageName, filePath)
        } else {
            DocumentRUtil.generateFileUri(packageName, filePath)
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
        } catch (_: Exception) {
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
        return if (OSUtil.atLeastT()) {
            DocumentTUtil.makeDirs(context, path, packageName)
        } else {
            DocumentRUtil.makeDirs(context, path)
        }
    }

    fun outTextToFile(
        context: Context,
        packageName: String,
        fileName: String,
        content: String,
        mimiType: String = "application/json"
    ): Boolean {
        return if (OSUtil.atLeastT()) {
            DocumentTUtil.outTextToFile(context, packageName, fileName, content, mimiType)
        } else {
            DocumentRUtil.outTextToFile(context, packageName, fileName, content, mimiType)
        }
    }


    fun deleteDocumentDir(
        context: Context, path: String
    ): Boolean {
        return runCatching {
            val paths = path.replace(Constant.ANDROID_DATA_PATH, "").split("/")
            val dataUri = Constant.ANDROID_DATA_URI.toUri()
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
        return if (OSUtil.atLeastT()) {
            DocumentTUtil.getFileUri(context, packageName, path)
        } else {
            DocumentRUtil.getFileUri(context, packageName, path)
        }
    }

    fun getFileUriOrCreate(
        context: Context,
        rootUri: Uri,
        childPath: String,
        fileName: String,
        mimeType: String = "application/json"
    ): Uri? {
        return runCatching {
            var documentFile = DocumentFile.fromTreeUri(context, rootUri)
            val childPaths = childPath.split("/")
            for (i in childPaths.indices) {
                if (childPaths[i].isEmpty()) continue
                documentFile =
                    getDocumentFile(documentFile!!, childPaths[i]) ?: documentFile.createDirectory(
                        childPaths[i])
            }
            val configFile = if (isChildExists(documentFile!!, fileName)) {
                documentFile.findFile(fileName)
            } else {
                documentFile.createFile(mimeType, fileName)
            }
            configFile?.uri
        }.getOrDefault(null)
    }

    fun getDocumentFile(
        context: Context, rootUri: Uri, childPath: String
    ): DocumentFile? {
        return runCatching {
            var documentFile = DocumentFile.fromTreeUri(context, rootUri)
            val childPaths = childPath.split("/")
            for (i in childPaths.indices) {
                if (childPaths[i].isEmpty()) continue
                documentFile =
                    getDocumentFile(documentFile!!, childPaths[i]) ?: documentFile.createDirectory(
                        childPaths[i])
            }
            documentFile
        }.getOrDefault(null)
    }


}