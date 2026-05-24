package me.simpleHook.core.constant

object ConfigConstant {
    const val ROOT_CUSTOM_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/customConfig.json"
    const val ROOT_EXTENSION_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/extensionConfig.json"

    const val RECORD_PATH = "/storage/self/primary/Android/media/%s/SimpleHook/record/record.log"
    const val RECORD_QUEUE_DIR = "/storage/self/primary/Android/media/%s/SimpleHook/record/queue"
    const val RECORD_QUEUE_READY_DIR =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/queue/ready"
    const val RECORD_QUEUE_TMP_DIR =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/queue/tmp"
    const val RECORD_QUEUE_STATS_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/record/queue/stats.json"
    const val TEMP_CONFIG_PATH = "/storage/self/primary/Android/media/me.simpleHook/cache/config.json"

    const val ROOT_DEX_PATH = "/storage/self/primary/Android/media/%s/SimpleHook/dex/"

    fun customRemoteConfigFileName(packageName: String) = "${packageName}_sh_custom_config.json"

    fun extensionRemoteConfigFileName(packageName: String) =
        "${packageName}_sh_extension_config.json"
}
