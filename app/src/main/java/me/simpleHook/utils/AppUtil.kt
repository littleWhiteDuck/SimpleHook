package me.simpleHook.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.net.toUri
import me.simpleHook.GlobalValue
import me.simpleHook.extension.showToast

@Suppress("DEPRECATION", "unused")
object AppUtil {
    private val pm by lazy { GlobalValue.packageManager }

    fun isAppInstalled(packageName: String): Boolean {
        return runCatching {
            pm.getPackageInfo(packageName, 0) != null
        }.getOrDefault(false)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getIcon(packageName: String): Drawable {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            pm.defaultActivityIcon
        }
    }

    fun getApps(): List<PackageInfo> {
        return pm.getInstalledPackages(0)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledSystemApp(): List<PackageInfo> {

        return pm.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledUserApp(): List<PackageInfo> {
        return pm.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0
        }
    }

    fun getUserPackageNames(): List<String> {
        val packs = mutableListOf<String>()
        pm.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0
        }.forEach {
            packs.add(it.packageName)
        }
        return packs
    }

    fun getSystemPackageNames(): List<String> {
        val packs = mutableListOf<String>()
        pm.getInstalledPackages(0).filter {
            (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
        }.forEach {
            packs.add(it.packageName)
        }
        return packs
    }

    fun getPackageNames(): List<String> {
        val packs = mutableListOf<String>()
        pm.getInstalledPackages(0).forEach {
            packs.add(it.packageName)
        }
        return packs
    }


    fun getTargetSdkVersion(packageName: String): Int {
        return runCatching {
            pm.getPackageInfo(packageName, 0).applicationInfo?.targetSdkVersion
                ?: -1
        }.getOrDefault(-1)
    }

    fun getAppName(packageName: String): String {
        return runCatching {
            getAppName(
                pm.getPackageInfo(
                    packageName,
                    0
                )
            )
        }.getOrDefault("NULL")
    }

    fun getAppName(packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo?.loadLabel(pm).toString()
    }


    fun getAppVersionName(packageName: String): String {
        return runCatching {
            pm.getPackageInfo(packageName, 0).versionName ?: "not define"
        }.getOrDefault("NULL")
    }

    fun getAppVersionCode(packageName: String): String {
        return try {
            val info = pm.getPackageInfo(packageName, 0)
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
            if (checkPackInfo(packageName)) {
                context.apply { startActivity(pm.getLaunchIntentForPackage(packageName)) }
            }
        } catch (_: java.lang.Exception) {
            context.showToast("可能应用被停用了,或者其他错误")
        }

    }

    private fun checkPackInfo(packageName: String): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = pm.getPackageInfo(packageName, 0)
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