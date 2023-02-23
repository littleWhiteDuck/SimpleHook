package me.simpleHook.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.compat.DocumentCompat.alterDocument
import me.simpleHook.compat.DocumentCompat.isChildExists
import me.simpleHook.compat.DocumentCompat.getDocumentFile
import me.simpleHook.constant.Constant

object DocumentRUtils {


    fun generateFileUri(packageName: String, filePath: String): Uri {
        val baseString =
            "${Constant.ANDROID_DATA_URI}/document/primary%3AAndroid%2Fdata%2F$packageName"
        val path = filePath.replace(Constant.ANDROID_DATA_PATH + packageName, "")
        return Uri.parse(baseString + path.replace("/", "%2F"))
    }


    fun makeDirs(context: Context, path: String): Boolean {
        return runCatching {
            val paths = path.replace(Constant.ANDROID_DATA_PATH, "").split("/")
            val rootUri = Uri.parse(Constant.ANDROID_DATA_URI)
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
            val paths = "$packageName/simpleHook/config".split("/")
            val rootUri = Uri.parse(Constant.ANDROID_DATA_URI)
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

    @Suppress("unused")
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

    @Suppress("UNUSED_PARAMETER")
    fun getFileUri(context: Context, packageName: String, path: String): Uri? {
        return runCatching {
            val rootUri = Uri.parse(Constant.ANDROID_DATA_URI)
            val paths = path.replace(Constant.ANDROID_DATA_PATH, "").split("/")
            var documentFile = DocumentFile.fromTreeUri(context, rootUri) ?: return null
            for (i in paths.indices) {
                if (paths[i].isEmpty()) continue
                documentFile = documentFile.findFile(paths[i]) ?: return null
            }
            if (documentFile.uri != rootUri) return documentFile.uri else return null
        }.getOrNull()
    }

}