package me.simpleHook.core.constant

object ConfigConstant {
    const val ROOT_CUSTOM_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/customConfig.json"
    const val ROOT_EXTENSION_CONFIG_PATH =
        "/storage/self/primary/Android/media/%s/SimpleHook/config/extensionConfig.json"

    const val RECORD_PATH = "/storage/self/primary/Android/media/%s/SimpleHook/record/record.log"
    const val TEMP_CONFIG_PATH = "/storage/self/primary/Android/media/me.simpleHook/cache/config.json"

    const val ROOT_DEX_PATH = "/storage/self/primary/Android/media/%s/SimpleHook/dex/"

    fun customRemoteConfigFileName(packageName: String) = "${packageName}_sh_custom_config.json"

    fun extensionRemoteConfigFileName(packageName: String) =
        "${packageName}_sh_extension_config.json"
}
