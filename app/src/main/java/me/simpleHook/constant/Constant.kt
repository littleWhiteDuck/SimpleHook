package me.simpleHook.constant

object Constant {
    // app 整体hook模式
    const val HOOK_ORIGIN = 0
    const val HOOK_JIA_GU = 1

    /* const val HOOK_360 = 1
     const val HOOK_TENCENT = 2
     const val HOOK_OTHER = 3*/
    // 方法Hook模式
    const val HOOK_RETURN = 0
    const val HOOK_PARAM = 1
    const val HOOK_BREAK = 2
    const val HOOK_STATIC_FIELD = 3
    const val HOOK_FIELD = 4
    const val HOOK_RECORD_PARAMS = 5
    const val HOOK_RECORD_RETURN = 6
    const val HOOK_RECORD_PARAMS_RETURN = 7

    //目录
    const val CONFIG_MAIN_DIRECTORY = "/data/simpleHook/"
    const val RECORD_TEMP_DIRECTORY = "logTemp/log.txt"
    const val CONFIG_DIRECTORY = "/storage/emulated/0/Download/simpleHook/data/"
    const val PRINT_LOG__DIRECTORY = "/storage/emulated/0/Download/simpleHook/printLog/"
    const val HOT_FIX_DIRECTORY = "/storage/emulated/0/Download/simpleHook/hotFix/"

    // Android/data
    const val ANDROID_DATA_URI =
        "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"
    const val ANDROID_DATA_PATH = "/storage/emulated/0/Android/data/"

    //应用列表排序
    const val APP_LIST_BY_NAME = 0
    const val APP_LIST_BY_PACKAGE_NAME = 1
    const val APP_LIST_BY_INSTALLED_TIME = 2
    const val APP_LIST_BY_TARGET_API = 3

    //两次点击间隔时间
    const val CLICK_TIME = 500L

    //Config: config.json、assistConfig.json
    const val APP_CONFIG_NAME = "config.json"
    const val EXTENSION_CONFIG_NAME = "assistConfig.json"

    //
    const val MODEL_EXTENSION_CONFIG = "模板配置"
}