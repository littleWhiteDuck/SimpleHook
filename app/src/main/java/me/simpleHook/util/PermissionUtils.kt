package me.simpleHook.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import me.simpleHook.App
import me.simpleHook.compat.DocumentCompatUtils

object PermissionUtils {

    private val sp by lazy { SPUtils(App.app) }

    private const val DATA_URI =
        "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"

    fun isGrantWritePermission(context: Context): Boolean {
        val permission = ActivityCompat.checkSelfPermission(
            context, "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        return permission == PackageManager.PERMISSION_GRANTED
    }

    fun verifyStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ), 1
        )
    }

    fun isGrantData(uri: String = DATA_URI): Boolean {
        return runCatching {
            for (persistedUriPermission in App.app.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission && persistedUriPermission.uri.toString() == uri) {
                    return true
                }
            }
            false
        }.getOrDefault(false)
    }

    fun isGrantPackage(packageName: String): Boolean {
        return isGrantData(DocumentCompatUtils.generateAppUri(packageName).toString())
    }

}