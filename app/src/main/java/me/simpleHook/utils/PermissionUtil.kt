package me.simpleHook.utils

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import me.simpleHook.App
import me.simpleHook.compat.DocumentCompat

object PermissionUtil {

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
            for (persistedUriPermission in App.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission && persistedUriPermission.uri.toString() == uri) {
                    return true
                }
            }
            false
        }.getOrDefault(false)
    }

    fun isGrantPackage(packageName: String): Boolean {
        return isGrantData(DocumentCompat.generateAppUri(packageName).toString())
    }

    fun takePersistableUriPermission(
        context: Context,
        uri: Uri?,
        takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    ) {
        if (uri == null || uri == Uri.EMPTY) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
    }

    fun takePersistableUriPermissions(
        context: Context,
        intent: Intent,
        takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    ) {
        intent.data?.let {
            takePersistableUriPermission(context, it, takeFlags)
            return
        }
        val clipData = intent.clipData ?: return
        for (index in 0 until clipData.itemCount) {
            takePersistableUriPermission(context, clipData.getItemAt(index).uri, takeFlags)
        }
    }

}
