package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import me.simpleHook.R
import java.lang.Exception


object AppUtils {

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getIcon(context: Context, packageName: String): Drawable {
        try {
            return context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return context.resources.getDrawable(R.drawable.ic_launcher_foreground, null)
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

    fun getAppName(context: Context, packageName: String): String {
        return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(
            context.packageManager
        )
            .toString()
    }

    fun getAppName(context: Context, packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
    }


    fun getAppVersionName(context: Context, packageName: String): String {
        var versionName = "未安装"
        try {
            versionName = context.packageManager.getPackageInfo(packageName, 0).versionName
        }catch (e:Exception){

        }
        return versionName
    }

}