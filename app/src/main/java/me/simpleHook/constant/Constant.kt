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

    //目录
    const val CONFIG_DIRECTORY = "/storage/emulated/0/Download/simpleHook/data/"
    const val PRINT_LOG__DIRECTORY = "/storage/emulated/0/Download/simpleHook/printLog/"
    const val HOT_FIX_DIRECTORY = "/storage/emulated/0/Download/simpleHook/hotFix/"

    //应用列表排序
    const val APP_LIST_BY_NAME = 0
    const val APP_LIST_BY_PACKAGE_NAME = 1
    const val APP_LIST_BY_INSTALLED_TIME = 2
    const val APP_LIST_BY_TARGET_API = 3

    //两次点击间隔时间
    const val CLICK_TIME = 500L
}