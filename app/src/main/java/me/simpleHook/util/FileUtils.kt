package me.simpleHook.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import java.io.*


object FileUtils {

    fun writeData(url: String, name: String, content: String) {
        val fileName = "${name}.json"
        writeJsonToFile(content, url, fileName)
    }

    private fun writeConfigFile(packageName: String, fileName: String, config: String) {
        if (config.isEmpty()) return
        val filePath = Constant.CONFIG_MAIN_DIRECTORY + packageName + "/config/"
        writeJsonToFile(config, filePath, fileName)
        SuUtil.set777()
        SuUtil.saveConfig(
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/", fileName, config
        )
        createLogFile()
    }

    fun writeJsonToFile(content: String, filePath: String, fileName: String) {
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
    /*private fun getDocumentFile(context: Context, pathStr: String): DocumentFile? {
        var path = pathStr
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        val path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
        return DocumentFile.fromSingleUri(
            context,
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2")
        )
    }*/

    fun realDeleteConfig(context: Context, packageName: String, name: String) {
        /* if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
             val configUri =
                 Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/simpleHook/$name"))
             try {
                 DocumentsContract.deleteDocument(context.contentResolver, configUri)
             } catch (e: java.lang.Exception) {

             }
         } else {
             deleteConfigFile(packageName, name)
         }*/
        deleteConfigFile(packageName, name)
        SuUtil.deleteConfig(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + name)
    }

    fun fakeDeleteConfig(context: Context, packageName: String, name: String) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val configUri =
                Uri.parse(changeToUri("/storage/emulated/0/Android/data/$packageName/simpleHook/$name"))
            try {
                alterDocument(context, configUri, "")
            } catch (e: java.lang.Exception) {

            }
        } else {
            writeConfigFile(packageName, name, "")
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
                documentFile.findFile(fileName);
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

    fun saveConfig(context: Context, packageName: String, fileName: String, content: String) {
        try {
            if (!AppUtils.isAppInstalled(context, packageName)) return
            writeConfigFile(packageName, fileName = fileName, config = content)
            /* if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                 writeDocumentFile(
                     context,
                     "/$packageName/simpleHook/",
                     fileName,
                     content,
                     "application/json"
                 )
             } else {
                 writeConfigFile(packageName, fileName = fileName, config = content)
             }*/
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
        val filePath = Constant.CONFIG_MAIN_DIRECTORY + packageName + "/config/" + fileName
        deleteFile(filePath)
    }

    private fun deleteFile(filePath: String) {
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
            FileWriter(file.path, true).use {
                it.write(strContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createLogFile() {
        try {
            val file = File(Constant.CONFIG_MAIN_DIRECTORY + Constant.RECORD_TEMP_DIRECTORY)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                SuUtil.set666()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    fun readLogFile(): List<PrintLog> {
        val filePath = Constant.CONFIG_MAIN_DIRECTORY + Constant.RECORD_TEMP_DIRECTORY
        val list = mutableListOf<PrintLog>()
        try {
            val file = File(filePath)
            file.useLines {
                it.iterator().forEach { str ->
                    try {
                        list.add(Gson().fromJson(str, PrintLog::class.java))
                    } catch (e: java.lang.Exception) {

                    }
                }
            }
            file.writer().use {
                it.write("")
            }
        } catch (e: Exception) {
            createLogFile()
        }
        return list
    }
}