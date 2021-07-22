package me.simpleHook.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import java.io.File

const val ALL_FILES_ACCESS_PERMISSION = 4

object FileUtils {

    fun writeData(url: String, name: String, content: String) {
        val fileName = "${name}.json"
        writeJsonToFile(content, url, fileName)
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
            file.writeText(strContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun verifyStoragePermissions(activity: Activity) {
        try {
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
}