package me.simpleHook.platform.hook.entry

import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.qauxv.loader.sbl.lsp101.Lsp101HookImpl
import io.github.qauxv.poststartup.StartupInfo
import io.github.qauxv.util.xpcompat.XposedBridge

/**
 * Entry point for libxpsoed API 101 (typically LSPosed).
 * 
 * 
 * The libxpsoed API is used as ART hook implementation.
 */
@RequiresApi(26)
class Lsp101HookEntry : XposedModule() {
    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        Lsp101HookImpl.init(this)
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        EzXHelperInit.initHandleLoadPackageLsp101(param)
        StartupInfo.setHookBridge(Lsp101HookImpl.INSTANCE)
        StartupInfo.setLoaderService(Lsp101HookImpl.INSTANCE)
        XposedBridge.log("onPackageReady@@@@@@@@@@")
        HookEntry.handleLoadPackage(param.packageName)
    }
}
