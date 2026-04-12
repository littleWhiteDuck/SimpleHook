package com.github.kyuubiran.ezxhelper.utils.parasitics

import android.app.Activity
import android.os.Bundle

/**
 * 所有需要在宿主内启动模块的Activity都必须继承于TransferActivity
 *
 * 或者 你可以继承自己的Activity 并将按照下面的方式重写这两个函数
 */
open class TransferActivity : Activity() {
    override fun getClassLoader(): ClassLoader {
        return FixedClassLoader(
            ActivityProxyManager.MODULE_CLASS_LOADER,
            ActivityProxyManager.HOST_CLASS_LOADER
        )
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val windowState = savedInstanceState.getBundle("android:viewHierarchyState")
        windowState?.let {
            it.classLoader = TransferActivity::class.java.classLoader!!
        }
        super.onRestoreInstanceState(savedInstanceState)
    }
}