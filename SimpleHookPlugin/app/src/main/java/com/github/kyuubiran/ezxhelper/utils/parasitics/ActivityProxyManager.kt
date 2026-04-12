package com.github.kyuubiran.ezxhelper.utils.parasitics

object ActivityProxyManager {
    /**
     * 用于区分宿主和模块intent
     */
    lateinit var ACTIVITY_PROXY_INTENT: String

    /**
     * 模块的包名 可以使用BuildConfig.APPLICATION_ID获取
     */
    lateinit var MODULE_PACKAGE_NAME_ID: String

    /**
     * 代理宿主的Activity类名
     */
    lateinit var HOST_ACTIVITY_PROXY_NAME: String

    /**
     * 模块类加载器 可以使用模块类名.javaclass.classLoader获取
     */
    lateinit var MODULE_CLASS_LOADER: ClassLoader

    /**
     * 宿主类加载器 可以使用AndroidAppHelper.currentApplication().classLoader获取
     */
    lateinit var HOST_CLASS_LOADER: ClassLoader
}