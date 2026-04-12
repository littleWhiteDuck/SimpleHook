package com.github.kyuubiran.ezxhelper.init

import android.app.AndroidAppHelper
import android.content.Context
import android.content.res.Resources
import android.content.res.XModuleResources
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.init.InitFields.hostPackageName
import com.github.kyuubiran.ezxhelper.init.InitFields.modulePath
import com.github.kyuubiran.ezxhelper.init.InitFields.moduleRes
import com.github.kyuubiran.ezxhelper.utils.*
import com.github.kyuubiran.ezxhelper.utils.parasitics.ActivityHelper
import com.github.kyuubiran.ezxhelper.utils.parasitics.ActivityProxyManager
import com.github.kyuubiran.ezxhelper.utils.parasitics.TransferActivity
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

object EzXHelperInit {
    /**
     * 使用本库必须执行的初始化
     * 应在handleLoadPackage方法内第一个调用
     * @see IXposedHookLoadPackage.handleLoadPackage
     * @see XC_LoadPackage.LoadPackageParam
     */
    fun initHandleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        setEzClassLoader(lpparam.classLoader)
        setHostPackageName(lpparam.packageName)
    }

    /**
     * 初始化Zygote 以便使用模块路径 和 模块资源
     * @see IXposedHookZygoteInit.initZygote
     */
    fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        moduleRes = XModuleResources.createInstance(modulePath, null)
    }

    /**
     * 设置本库使用的类加载器
     *
     * 注意：通常情况下 建议使用框架提供的类加载器进行操作
     *
     * 但某些app会修改自身的类加载器 遇到这种情况请自行设置运行时的类加载器
     * @param classLoader 类加载器
     */
    fun setEzClassLoader(classLoader: ClassLoader) {
        ezXClassLoader = classLoader
    }

    /**
     * 设置宿主包名
     */
    fun setHostPackageName(packageName: String) {
        hostPackageName = packageName
    }

    /**
     * 初始化全局ApplicationContext
     * @param context ctx
     * @param addPath 是否往ctx中添加模块资源路径
     * @param initModuleResources 是否初始化moduleRes
     */
    fun initAppContext(
        context: Context = AndroidAppHelper.currentApplication(),
        addPath: Boolean = false,
        initModuleResources: Boolean = false
    ) {
        appContext = context
        if (addPath) addModuleAssetPath(appContext)
        if (initModuleResources) moduleRes = context.resources
    }


    /**
     * 设置自定义的 Logger,与 setLogXp setLogTag setToastTag 冲突！
     */
    fun setLogger(log: Logger) {
        Log.currentLogger = log
    }


    /**
     * 设置是否输出日志到 Xposed
     */
    fun setLogXp(toXp: Boolean) {
        Log.currentLogger.logXp = toXp
    }

    /**
     * 设置打印日志的标签
     */
    fun setLogTag(tag: String) {
        Log.currentLogger.logTag = tag
    }

    /**
     * 设置Log.toast的Tag
     * 如果不设置会使用日志TAG
     * @see Log.toast
     */
    fun setToastTag(tag: String) {
        Log.currentLogger.toastTag = tag
    }

    /**
     * 将模块的资源路径添加到Context.resources内 允许直接以R.xx.xxx获取资源
     *
     * 要求:
     *
     * 1.在项目的build.gradle中修改资源id(不与宿主冲突即可) 如下:
     *
     * Kotlin Gradle DSL:
     * androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
     *
     * Groovy:
     * aaptOptions.additionalParameters '--allow-reserved-package-id', '--package-id', '0x64'
     *
     * 2.执行过EzXHelperInit.initZygote
     *
     * 3.在使用资源前调用
     *
     * eg:在Activity中:
     * init { addModuleAssetPath(this) }
     *
     * @see initZygote
     *
     */
    fun addModuleAssetPath(context: Context) {
        addModuleAssetPath(context.resources)
    }

    fun addModuleAssetPath(resources: Resources) {
        resources.assets.invokeMethod(
            "addAssetPath",
            args(modulePath),
            argTypes(String::class.java)
        )
    }

    /**
     * 初始化启动未注册Activity所需的各类内容
     * 需要在initSubActivity之前调用 且不能过早调用
     *
     * @param modulePackageName 模块包名
     * @param hostActivityProxyName 代理的宿主的Activity名
     * @param moduleClassLoader 模块的类加载器
     * @param hostClassLoader 宿主的类加载器
     * @see initSubActivity
     * @see ActivityProxyManager
     * @see TransferActivity
     */
    fun initActivityProxyManager(
        modulePackageName: String,
        hostActivityProxyName: String,
        moduleClassLoader: ClassLoader,
        hostClassLoader: ClassLoader = AndroidAppHelper.currentApplication().classLoader!!
    ) {
        ActivityProxyManager.MODULE_PACKAGE_NAME_ID = modulePackageName
        ActivityProxyManager.ACTIVITY_PROXY_INTENT =
            "${modulePackageName.replace('.', '_')}_intent_proxy"
        ActivityProxyManager.HOST_ACTIVITY_PROXY_NAME = hostActivityProxyName
        ActivityProxyManager.MODULE_CLASS_LOADER = moduleClassLoader
        ActivityProxyManager.HOST_CLASS_LOADER = hostClassLoader
    }

    /**
     * 初始化启动未注册的Activity
     *
     * 需要先调用initActivityProxyManager
     * @see initActivityProxyManager
     *
     * 需要使用addModuleAssetPath 所以必须调用initZygote
     * @see initZygote
     * @see addModuleAssetPath
     *
     * 需要在Application.onCreate之后调用 且只需要调用一次
     * 并且模块所有的Activity需要继承于TransferActivity
     */
    fun initSubActivity() {
        ActivityHelper.initSubActivity()
    }
}
