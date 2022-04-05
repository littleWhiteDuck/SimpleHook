package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build


object AppUtils {
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getIcon(context: Context, packageName: String): Drawable {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            context.packageManager.defaultActivityIcon
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledSystemApp(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledUserApp(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }


    fun getAppIcon(context: Context, packageInfo: PackageInfo): Drawable {
        return packageInfo.applicationInfo.loadIcon(context.packageManager)
    }

    fun getTargetSdkVersion(context: Context, packageName: String): String {
        return try {
            "Target Api ${
                context.packageManager.getPackageInfo(
                    packageName, 0
                ).applicationInfo.targetSdkVersion
            }"
        } catch (e: Exception) {
            "Target Api -1"
        }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(
                context.packageManager
            ).toString()
        } catch (e: java.lang.Exception) {
            "未获取到"
        }
    }

    fun getAppName(context: Context, packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
    }


    fun getAppVersionName(context: Context, packageName: String): String {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "未安装"
        }
    }

    fun getAppVersionCode(context: Context, packageName: String): String {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode
            }
            versionCode.toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun startApp(packageName: String, context: Context) {
        try {
            if (checkPackInfo(packageName, context)) {
                context.apply { startActivity(packageManager.getLaunchIntentForPackage(packageName)) }
            }
        } catch (e: java.lang.Exception) {
            "可能应用被停用了,或者其他错误".toast(context)
        }

    }

    private fun checkPackInfo(packageName: String, context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return packageInfo != null
    }

}