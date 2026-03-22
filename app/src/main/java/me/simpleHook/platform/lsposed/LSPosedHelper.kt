package me.simpleHook.platform.lsposed

import android.util.Log
import io.github.libxposed.service.XposedService

object LSPosedHelper {
    private var service: XposedService? = null

    fun setService(service: XposedService?) {
        this.service = service
        Log.d("LSPosedHelper", "setService: ${service?.apiVersion}")
    }

    /**
     * 判断模块是否已在LSPosed new api下激活
     * API 101直接看是否能获取到service即可
     * API 100的部分版本会存在总开关关闭但还可以获取到service的情况，这时候通过scope粗糙判断，并不准确
     */
    fun isActivated() = if (service != null) {
        service!!.apiVersion >= 101 ||
                service!!.scope.toSet().isNotEmpty()
    } else false

    fun isInScope(packageName: String): Boolean {
        Log.d("LSPosedHelper", "isInScope: $packageName,${service?.scope}")
        return service?.scope?.contains(packageName) == true
    }

    fun requestScopeIfNeeded(packageName: String) {
        Log.d("LSPosedHelper", "requestScopeIfNeeded: $packageName")
        if (!isInScope(packageName)) {
            service?.requestScope(
                listOf(packageName),
                object : XposedService.OnScopeEventListener {})
        }
    }

    fun removeScopeIfNeeded(packageName: String) {
        if (isInScope(packageName)) {
            service?.removeScope(listOf(packageName))
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
                    service?.requestScope(listOf(it), object : XposedService.OnScopeEventListener {})
                }
            }
        } else {
            packageNames.distinct().forEach {
                if (scopeSet.contains(it)) {
                    service?.removeScope(listOf(it))
                }
            }
        }
    }

}
