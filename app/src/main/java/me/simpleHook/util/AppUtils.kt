package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import me.simpleHook.GlobalValue
import me.simpleHook.extension.showToast
import androidx.core.net.toUri

@Suppress("DEPRECATION", "unused")
object AppUtils {

    fun isAppInstalled(packageName: String): Boolean {
        return runCatching {
            GlobalValue.packageManager.getPackageInfo(packageName, 0) != null
        }.getOrDefault(false)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getIcon(context: Context, packageName: String): Drawable {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            context.packageManager.defaultActivityIcon
        }
    }

    fun getApps(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(0)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledSystemApp(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledUserApp(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0
        }
    }

    fun getUserPackageNames(context: Context): List<String> {
        val packageManager = context.packageManager
        val packs = mutableListOf<String>()
        packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0
        }.forEach {
            packs.add(it.packageName)
        }
        return packs
    }

    fun getSystemPackageNames(context: Context): List<String> {
        val packageManager = context.packageManager
        val packs = mutableListOf<String>()
        packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
        }.forEach {
            packs.add(it.packageName)
        }
        return packs
    }

    fun getPackageNames(context: Context): List<String> {
        val packageManager = context.packageManager
        val packs = mutableListOf<String>()
        packageManager.getInstalledPackages(0).forEach {
            packs.add(it.packageName)
        }
        return packs
    }


    fun getTargetSdkVersion(context: Context, packageName: String): Int {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0).applicationInfo?.targetSdkVersion
                ?: -1
        }.getOrDefault(-1)
    }

    fun getAppName(context: Context, packageName: String): String {
        return runCatching {
            getAppName(
                context, context.packageManager.getPackageInfo(
                    packageName,
                    0
                )
            )
        }.getOrDefault("NULL")
    }

    fun getAppName(context: Context, packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo?.loadLabel(context.packageManager).toString()
    }


    fun getAppVersionName(context: Context, packageName: String): String {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0).versionName ?: "not define"
        }.getOrDefault("NULL")
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
        } catch (_: java.lang.Exception) {
            context.showToast("可能应用被停用了,或者其他错误")
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

    fun jumpAppInfoPage(context: Context, packageName: String) {
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = "package:$packageName".toUri()
        context.startActivity(intent)
    }

}