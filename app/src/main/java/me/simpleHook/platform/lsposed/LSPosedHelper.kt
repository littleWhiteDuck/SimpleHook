package me.simpleHook.platform.lsposed

import io.github.libxposed.service.XposedService

object LSPosedHelper {
    private var service: XposedService? = null

    fun setService(service: XposedService?) {
        this.service = service
    }

    /**
     * 判断模块是否已在LSPosed new api下激活
     * API 101直接看是否能获取到service即可
     * API 100的部分版本会存在总开关关闭但还可以获取到service的情况，这时候通过scope粗糙判断，并不准确
     */
    fun isActivated() = service != null

    fun filterNotInScope(packageNames: List<String>): List<String> {
        val scope = service?.scope ?: return packageNames
        return packageNames.filter { !scope.contains(it) }
    }

    fun requestScopes(packageNames: List<String>) {
        val filtered = filterNotInScope(packageNames) // 避免已经在作用域还请求授权
        if (filtered.isEmpty()) return
        try {
            service?.requestScope(
                filtered,
                object : XposedService.OnScopeEventListener {})
        } catch (e: XposedService.ServiceException) {
            e.printStackTrace()
        }
    }

    fun removeScopes(packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        try {
            service?.removeScope(packageNames)
        } catch (e: XposedService.ServiceException) {
            e.printStackTrace()
        }
    }

}
