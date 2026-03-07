package me.simpleHook.platform.lsposed

import io.github.libxposed.service.XposedService

object LSPosedHelper {
    private var service: XposedService? = null

    fun setService(service: XposedService?) {
        this.service = service
    }

    /**
     * 判断模块是否已在LSPosed new api下激活
     * 不准确，存在勾选了若干scope但总开关未打开的情况
     */
    fun isActivated() = if (service != null) {
        service!!.scope.toSet().isNotEmpty()
    } else false

    fun isInScope(packageName: String): Boolean {
        return service?.scope?.contains(packageName) == true
    }

    fun requestScopeIfNeeded(packageName: String) {
        if (!isInScope(packageName)) {
            service?.requestScope(packageName, object : XposedService.OnScopeEventListener {})
        }
    }

    fun removeScopeIfNeeded(packageName: String) {
        if (isInScope(packageName)) {
            service?.removeScope(packageName)
        }
    }

    fun changeScope(packageName: String, isAdd: Boolean) {
        if (isAdd) addScope(arrayOf(packageName))
        else removeScope(arrayOf(packageName))
    }

    fun addScope(packageNames: Array<String>) {
        changeScope(packageNames, true)
    }

    fun removeScope(packageNames: Array<String>) {
        changeScope(packageNames, false)
    }

    @Synchronized
    private fun changeScope(
        packageNames: Array<String>, isAdd: Boolean
    ) {
        val scopeSet = service?.scope?.toSet().orEmpty()
        if (isAdd) {
            packageNames.distinct().forEach {
                if (!scopeSet.contains(it)) {
                    service?.requestScope(it, object : XposedService.OnScopeEventListener {})
                }
            }
        } else {
            packageNames.distinct().forEach {
                if (scopeSet.contains(it)) {
                    service?.removeScope(it)
                }
            }
        }
    }

}
