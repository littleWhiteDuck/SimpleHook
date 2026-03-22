package me.simpleHook.platform.hook.entry

import androidx.annotation.Keep
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.qauxv.loader.sbl.xp51.Xp51HookImpl
import io.github.qauxv.poststartup.StartupInfo

/**
 * Entry point for started Xposed API 51-99.
 * 
 * 
 * Xposed is used as ART hook implementation.
 */
@Keep
class Xp51HookEntry : IXposedHookLoadPackage {

    @Keep
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        StartupInfo.setHookBridge(Xp51HookImpl.INSTANCE)
        StartupInfo.setLoaderService(Xp51HookImpl.INSTANCE)

        HookEntry.handleLoadPackage(lpparam.packageName)
    }

}
