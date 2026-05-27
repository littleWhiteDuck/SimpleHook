package me.simpleHook.core.constant

object ConfigConstant {
    const val ROOT_CUSTOM_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/customConfig.json"
    const val ROOT_EXTENSION_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/extensionConfig.json"

    const val RECORD_DIR = "/storage/self/primary/Android/media/%s/SimpleHook/record"
    const val RECORD_SOURCE_DIR =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/%s"
    const val RECORD_SOURCE_READY_DIR =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/%s/ready"
    const val RECORD_SOURCE_TMP_DIR =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/%s/tmp"
    const val RECORD_SOURCE_STATS_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/%s/stats.json"
    const val RECORD_SOURCE_MANIFEST_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/%s/manifest.txt"
    const val TEMP_CONFIG_PATH = "/storage/self/primary/Android/media/me.simpleHook/cache/config.json"

    const val ROOT_DEX_PATH = "/storage/self/primary/Android/media/%s/SimpleHook/dex/"

    fun customRemoteConfigFileName(packageName: String) = "${packageName}_sh_custom_config.json"

    fun extensionRemoteConfigFileName(packageName: String) =
        "${packageName}_sh_extension_config.json"
}
