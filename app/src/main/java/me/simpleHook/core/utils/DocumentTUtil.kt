package me.simpleHook.core.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.core.compat.DocumentCompat.alterDocument
import me.simpleHook.core.compat.DocumentCompat.changeToUri
import me.simpleHook.core.compat.DocumentCompat.generateAppUri
import me.simpleHook.core.compat.DocumentCompat.getDocumentFile
import me.simpleHook.core.compat.DocumentCompat.isChildExists
import me.simpleHook.core.constant.Constant
import androidx.core.net.toUri

object DocumentTUtil {


    fun generateFileUri(packageName: String, filePath: String): Uri {
        val baseString =
            "${Constant.ANDROID_DATA_URI}%2F$packageName/document/primary%3AAndroid%2Fdata%2F$packageName"
        val path = filePath.replace(Constant.ANDROID_DATA_PATH + packageName, "")
        return (baseString + path.replace("/", "%2F")).toUri()
    }


    fun makeDirs(context: Context, path: String, packageName: String): Boolean {
        return runCatching {
            val paths = path.replace(Constant.ANDROID_DATA_PATH + packageName, "").split("/")
            val rootUri = generateAppUri(packageName)
            var documentFile = DocumentFile.fromTreeUri(context, rootUri)
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = getDocumentFile(documentFile!!, paths[i])
                    ?: documentFile.createDirectory(paths[i])
            }
            true
        }.getOrDefault(false)
    }


    fun outTextToFile(
        context: Context,
        packageName: String,
        fileName: String,
        content: String,
        mimiType: String = "application/json"
    ): Boolean {
        return runCatching {
            val paths = "simpleHook/config".split("/")
            val rootUri = changeToUri(Constant.ANDROID_DATA_PATH + packageName).toUri()
            var documentFile = DocumentFile.fromTreeUri(context, rootUri)
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = getDocumentFile(documentFile!!, paths[i])
                    ?: documentFile.createDirectory(paths[i])
            }
            val configFile = if (isChildExists(documentFile!!, fileName)) {
                documentFile.findFile(fileName)
            } else {
                documentFile.createFile(mimiType, fileName)
            }
            configFile?.uri?.let { alterDocument(context, it, content) } ?: false
        }.getOrDefault(false)
    }


    fun getFileUri(context: Context, packageName: String, path: String): Uri? {
        return runCatching {
            val rootUri = generateAppUri(packageName)
            val paths = path.replace(Constant.ANDROID_DATA_PATH + packageName, "").split("/")
            var documentFile = DocumentFile.fromTreeUri(context, rootUri) ?: return null
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = documentFile.findFile(paths[i]) ?: return null
            }
            if (documentFile.uri != rootUri) return documentFile.uri else return null
        }.getOrNull()
    }


}