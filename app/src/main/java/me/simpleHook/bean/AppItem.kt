package me.simpleHook.bean

/**
 * 此类用于appList中的显示
 * @param name appName
 * @param packageName 包名
 * @param versionName 版本名
 * @param installedTime 最后一次安装时间
 */

data class AppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val installedTime: String,
    val targetApi: Int
)