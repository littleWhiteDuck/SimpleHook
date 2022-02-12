package me.simpleHook.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import me.simpleHook.constant.Constant
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


object FileUtils {

    fun writeData(url: String, name: String, content: String) {
        val fileName = "${name}.json"
        writeJsonToFile(content, url, fileName)
    }

    fun createConfigFile(packageName: String, config: String, isAppConfig: Boolean = true) {
        val filePath = Constant.CONFIG_DIRECTORY + packageName + "/"
        val fileName = if (isAppConfig) "config.json" else "assistConfig.json"
        writeJsonToFile(config, filePath, fileName)
    }

    private fun writeConfigFile(packageName: String, fileName: String, config: String) {
        val filePath = "/storage/emulated/0/Android/data/$packageName/"
        writeJsonToFile(config, filePath, fileName)
    }

    private fun writeJsonToFile(content: String, filePath: String, fileName: String) {
        makeFilePath(filePath, fileName)
        val strFilePath = filePath + fileName
        val strContent = "${content}\r\n"
        try {
            val file = File(strFilePath)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            file.writer().use { it.write(strContent) }
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
                context,
                "android.permission.WRITE_EXTERNAL_STORAGE"
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

    private fun getDocumentFile(context: Context, pathStr: String): DocumentFile? {
        var path = pathStr
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        val path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
        return DocumentFile.fromSingleUri(
            context,
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2")
        )
    }

    fun realDeleteConfig(context: Context, packageName: String, name: String) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val configUri =
                Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/$name"))
            try {
                DocumentsContract.deleteDocument(context.contentResolver, configUri)
            } catch (e: java.lang.Exception) {

            }
        } else {
            deleteFile(packageName, name)
        }
    }

    fun fakeDeleteConfig(context: Context, packageName: String, name: String) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val configUri =
                Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/$name"))
            try {
                alterDocument(context, configUri, "")
            } catch (e: java.lang.Exception) {
                Handler(Looper.getMainLooper()).post {
                    "错误".toast(context)
                }
            }
        } else {
            writeConfigFile(packageName, name, "")
        }

    }

    fun saveConfig(context: Context, packageName: String, name: String, content: String) {
        /*val encodeContent = Base64.encodeToString(content.toByteArray(), Base64.NO_PADDING)*/
        try {
            if (!AppUtils.isAppInstalled(context, packageName)) return
            val path = "/storage/emulated/0/Android/data/$packageName/"
            val packUri = Uri.parse(changeToUri(path))
            val myUri =
                Uri.parse(changeToUri("/storage/emulated/0/Android/data/me.simpleHook/"))
            val configUri = Uri.parse(changeToUri("$path/$name"))
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val configDocumentFile = DocumentFile.fromSingleUri(context, configUri)
                val fileUri: Uri? = if (!configDocumentFile!!.exists()) {
                    val documentFile = getDocumentFile(context, path)
                    if (!documentFile!!.exists()) {
                        val documentFile2 = DocumentFile.fromTreeUri(context, myUri)
                        documentFile2!!.createDirectory(packageName)
                    }
                    DocumentsContract.createDocument(
                        context.contentResolver,
                        packUri,
                        "application/json",
                        name
                    )
                } else {
                    configUri
                }
                if (content.isNotEmpty()) {
                    fileUri?.let { alterDocument(context, it, content) } ?: "写入 $name 错误".toast(
                        context
                    )
                }
            } else {
                writeConfigFile(packageName, fileName = name, config = content)
            }
        } catch (e: Exception) {
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
                activity,
                arrayOf(
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                ), 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        /*       try {
                   val permission = ActivityCompat.checkSelfPermission(
                       activity,
                       "android.permission.WRITE_EXTERNAL_STORAGE"
                   )
                   if (permission != PackageManager.PERMISSION_GRANTED) {
                       ActivityCompat.requestPermissions(
                           activity,
                           arrayOf(
                               "android.permission.READ_EXTERNAL_STORAGE",
                               "android.permission.WRITE_EXTERNAL_STORAGE"
                           ), 1
                       )
                   }
               } catch (e: Exception) {
                   e.printStackTrace()
               }
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                   val builder = AlertDialog.Builder(activity)
                       .setTitle("提示")
                       .setMessage("因为你的系统版本不在Android11以下，所以需要获取全部文件管理权限")
                       .setPositiveButton("去获取") { _, _ ->
                           val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                           activity.startActivityForResult(intent, ALL_FILES_ACCESS_PERMISSION)
                       }
                       .setNegativeButton("取消", null)
                   builder.show()
               }*/
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

    private fun makeRootDirectory(filePath: String) {
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

    private fun deleteFile(packageName: String, fileName: String) {
        val filePath =
            Constant.CONFIG_DIRECTORY + packageName + fileName
        val file = File(filePath)
        if (file.exists()) file.delete()
    }
}