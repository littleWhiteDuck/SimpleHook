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

    //目录
    const val MAIN_DIRECTORY = "/storage/emulated/0/simpleHook/"
    const val CONFIG_DIRECTORY = "/storage/emulated/0/simpleHook/data/"
}