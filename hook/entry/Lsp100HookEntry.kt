package me.simpleHook.platform.hook.entry

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.qauxv.loader.sbl.lsp100.Lsp100HookImpl
import io.github.qauxv.poststartup.StartupInfo
import me.simpleHook.BuildConfig
import me.simpleHook.platform.hook.utils.HookHelper

class Lsp100HookEntry(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        Lsp100HookImpl.init(this)
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        EzXHelperInit.initHandleLoadPackageLsp100(param)
        StartupInfo.setHookBridge(Lsp100HookImpl.INSTANCE)
        StartupInfo.setLoaderService(Lsp100HookImpl.INSTANCE)

        if (param.packageName == BuildConfig.APPLICATION_ID) {
            // 经测试，似乎LSPosed new api有时候会hook模块自身，故保留这段代码
            findMethod("me.simpleHook.feature.main.ui.MainActivity") {
                name == "isModuleLive"
            }.hookReturnConstant(true)
        } else {
            if (HookHelper.isAppContextInitialized) return
            findMethod(Application::class.java) {
                name == "attach"
            }.hookAfter {
                HookHelper.initFields(
                    context = it.args[0] as Context,
                    packageName = param.packageName,
                    applicationName = it.thisObject.javaClass.name
                )
                HookHelper.appContext.getExternalFilesDirs(null)
                HookInit.startHook()
            }
        }
    }
}