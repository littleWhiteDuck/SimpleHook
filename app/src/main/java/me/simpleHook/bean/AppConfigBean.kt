package me.simpleHook.bean

import androidx.annotation.Keep

/**
 * 应用配置类
 * @param appName 应用名
 * @param packageName 包名
 * @param mode 普通模式、加固模式
 * @param description 描述
 * @param versionName 版本名
 * @param config
 * @param canUse 是否可用
 */
@Keep
data class AppConfigBean(
    val appName: String,
    val packageName: String,
    val mode: Int,
    val description: String,
    val versionName: String,
    val config: ArrayList<ConfigBean>,
    val canUse:Boolean = true
)
