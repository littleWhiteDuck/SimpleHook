package me.simpleHook.platform.hook.entry;

import androidx.annotation.Keep;

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.qauxv.loader.sbl.xp51.Xp51HookImpl;
import io.github.qauxv.poststartup.StartupInfo;

/**
 * Entry point for started Xposed API 51-99.
 * <p>
 * Xposed is used as ART hook implementation.
 */
@Keep
public class Xp51HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static XC_LoadPackage.LoadPackageParam sLoadPackageParam = null;
    private static IXposedHookZygoteInit.StartupParam sInitZygoteStartupParam = null;
    private static String sModulePath = null;

    public static String sCurrentPackageName = null;

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Keep
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws ReflectiveOperationException {
        EzXHelperInit.INSTANCE.initHandleLoadPackage(lpparam);
        StartupInfo.setHookBridge(Xp51HookImpl.INSTANCE);
        StartupInfo.setLoaderService(Xp51HookImpl.INSTANCE);

        HookEntry.INSTANCE.handleLoadPackage(lpparam.packageName);
    }

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Override
    public void initZygote(StartupParam startupParam) {
        sInitZygoteStartupParam = startupParam;
        sModulePath = startupParam.modulePath;
    }

    /**
     * Get the {@link XC_LoadPackage.LoadPackageParam} of the current module.
     *
     * @return the lpparam
     */
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (sLoadPackageParam == null) {
            throw new IllegalStateException("LoadPackageParam is null");
        }
        return sLoadPackageParam;
    }

    /**
     * Get the path of the current module.
     *
     * @return the module path
     */
    public static String getModulePath() {
        if (sModulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return sModulePath;
    }

    /**
     * Get the {@link IXposedHookZygoteInit.StartupParam} of the current module.
     *
     * @return the initZygote param
     */
    public static IXposedHookZygoteInit.StartupParam getInitZygoteStartupParam() {
        if (sInitZygoteStartupParam == null) {
            throw new IllegalStateException("InitZygoteStartupParam is null");
        }
        return sInitZygoteStartupParam;
    }

}
