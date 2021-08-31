package me.simpleHook.bean

/**
 * 应用配置类
 * @param appName 应用名
 * @param packageName 包名
 * @param mode 普通模式、加固模式
 * @param description 描述
 * @param versionName 版本名
 * @param config 具体的配置
 * @param canUse 是否可用
 */
data class AppConfigBean(
    val appName: String,
    val packageName: String,
    val mode: Int,
    val description: String,
    val versionName: String,
    val config: String,
    val canUse:Boolean = true
) {
    override fun toString(): String {
        return "{\"appName\":\"$appName\",\"packageName\":\"$packageName\",\"versionName\":\"$versionName\"," +
                "\"mode\":$mode,\"description\":\"$description\",$config,\"canUse\":$canUse}"
    }
}
