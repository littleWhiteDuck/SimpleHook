package me.simpleHook.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.constant.Constant
import me.simpleHook.constant.Constant.ROOT_CONFIG_MAIN_DIRECTORY
import me.simpleHook.database.entity.PrintLog
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            for (persistedUriPermission in context.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission && persistedUriPermission.uri.toString() == "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata") {
                    return true
                }
            }
            return false
        } else {
            val permission = ActivityCompat.checkSelfPermission(
                context, "android.permission.WRITE_EXTERNAL_STORAGE"
            )
            return permission == PackageManager.PERMISSION_GRANTED
        }
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

    fun realDeleteConfig(context: Context, packageName: String, name: String) {
        if (FlavorUtils.isNormal()) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val simpleHookUri =
                    Uri.parse("/storage/emulated/0/Android/data/$packageName/simpleHook")
                val configUri =
                    Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/simpleHook/config/${Constant.APP_CONFIG_NAME}"))
                val extensionConfigUri =
                    Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/simpleHook/config/${Constant.EXTENSION_CONFIG_NAME}"))
                try {
                    if (name == Constant.APP_CONFIG_NAME) {
                        if (DocumentFile.fromSingleUri(context, extensionConfigUri)
                                ?.exists() == true
                        ) {
                            DocumentsContract.deleteDocument(context.contentResolver, configUri)
                        } else {
                            deleteDocumentDirection(context, "/$packageName/simpleHook/")
                        }
                    } else {
                        if (DocumentFile.fromSingleUri(context, configUri)?.exists() == true) {
                            DocumentsContract.deleteDocument(
                                context.contentResolver, extensionConfigUri
                            )
                        } else {
                            deleteDocumentDirection(context, "/$packageName/simpleHook/")
                        }
                    }

                } catch (e: java.lang.Exception) {

                }
            } else {
                deleteConfigFile(packageName, name)
            }
        } else {
            deleteConfigFile(packageName, name)
        }
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


    @Synchronized
    fun saveConfig(context: Context, packageName: String, fileName: String, content: String) {
        try {
            if (!AppUtils.isAppInstalled(context, packageName) || content.isEmpty()) return
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                if (SuUtil.isRoot) {
                    SuUtil.saveConfig(
                        filePath = "$ROOT_CONFIG_MAIN_DIRECTORY$packageName/config/",
                        fileName = fileName,
                        content = content.replace("\\", "\\\\")
                    )
                } else {
                    Handler(Looper.getMainLooper()).post {
                        "get ROOT failed".toast(context)
                    }
                }
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                writeDocumentFile(
                    context,
                    "/$packageName/simpleHook/config/",
                    fileName,
                    content,
                    "application/json"
                )
            } else {
                val filePath = Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY
                writeTextToFile(content, filePath, fileName)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                "这应该是出现了bug".toast(context)
            }
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

    private fun deleteConfigFile(packageName: String, fileName: String) {
        val configPath =
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.APP_CONFIG_NAME
        val extensionPath =
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.EXTENSION_CONFIG_NAME
        if (FlavorUtils.isNormal()) {
            if (fileName == Constant.APP_CONFIG_NAME) {
                if (fileIsExists(extensionPath)) {
                    deleteFile(configPath)
                } else {
                    deleteFile(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/")
                }
            } else {
                if (fileIsExists(configPath)) {
                    deleteFile(extensionPath)
                } else {
                    deleteFile(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/")
                }
            }
        } else {
            if (fileName == Constant.APP_CONFIG_NAME) {
                SuUtil.deleteConfig(ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.APP_CONFIG_NAME}")
                /* if (fileIsExists(extensionPath)) {
                     // deleteFile(configPath)
                     //  SuUtil.deleteConfig(configPath)
                     SuUtil.deleteConfig(ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.APP_CONFIG_NAME}")
                 } else {
                     // SuUtil.deleteConfig(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook")
                     SuUtil.deleteConfig("$ROOT_CONFIG_MAIN_DIRECTORY$packageName")
                 }*/
            } else {
                SuUtil.deleteConfig(ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.EXTENSION_CONFIG_NAME}")
                /*if (fileIsExists(configPath)) {
                    //deleteFile(extensionPath)
                    //SuUtil.deleteConfig(extensionPath)
                    SuUtil.deleteConfig(ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.EXTENSION_CONFIG_NAME}")
                } else {
                    //SuUtil.deleteConfig(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook")
                    SuUtil.deleteConfig("$ROOT_CONFIG_MAIN_DIRECTORY$packageName")
                }*/
            }

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
            if (file.length() > 12 * 1000 * 1000) {
                file.delete()
            }
            FileWriter(file.path, true).use {
                it.write(strContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readLogFile(context: Context, packageName: String): List<PrintLog> {
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
        val fileUri = Uri.parse(changeToUri(filePath))
        val recordPath = ROOT_CONFIG_MAIN_DIRECTORY + Constant.RECORD_TEMP_DIRECTORY
        val list = mutableListOf<PrintLog>()
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                SuUtil.copyFile(
                    filePath, ROOT_CONFIG_MAIN_DIRECTORY + "logTemp"
                )
                SuUtil.deleteFile(filePath)
                try {
                    val file = File(recordPath)
                    file.useLines {
                        it.iterator().forEach { str ->
                            try {
                                list.add(Gson().fromJson(str, PrintLog::class.java))
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    SuUtil.deleteFile(recordPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                context.contentResolver.openInputStream(fileUri)?.also { inputStream ->
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    bufferedReader.useLines {
                        it.iterator().forEach { str ->
                            try {
                                list.add(Gson().fromJson(str, PrintLog::class.java))
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                DocumentsContract.deleteDocument(context.contentResolver, fileUri)
            } else {
                File(filePath).useLines {
                    it.iterator().forEach { str ->
                        try {
                            list.add(Gson().fromJson(str, PrintLog::class.java))
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                deleteFile(filePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: OutOfMemoryError) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                SuUtil.deleteFile(filePath)
                SuUtil.deleteFile(recordPath)
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                DocumentsContract.deleteDocument(context.contentResolver, fileUri)
            } else {
                deleteFile(filePath)
            }
            context.getString(R.string.record_tip_log_out_of_memory).toast(context)
        }
        return list
    }
}