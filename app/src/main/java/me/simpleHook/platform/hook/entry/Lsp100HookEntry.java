package me.simpleHook.platform.hook.entry;

import static com.github.kyuubiran.ezxhelper.utils.HookUtilsKt.hookReturnConstant;
import static com.github.kyuubiran.ezxhelper.utils.MethodUtilsKt.findMethod;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.XposedApiExact;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import io.github.qauxv.loader.sbl.lsp100.Lsp100HookImpl;
import io.github.qauxv.poststartup.StartupInfo;
import kotlin.jvm.functions.Function1;
import me.simpleHook.BuildConfig;
import me.simpleHook.platform.hook.utils.HookHelper;

/**
 * Entry point for libxpsoed API 100 (typically LSPosed).
 * <p>
 * The libxpsoed API is used as ART hook implementation.
 */
@XposedApiExact(100)
public class Lsp100HookEntry implements Lsp10xHookEntryHandler {

    private final XposedModule self;
    private XposedModule.ModuleLoadedParam mModule;

    /**
     * Instantiates a new Xposed module.
     * <p>
     * When the module is loaded into the target process, the constructor will be called.
     *
     * @param self  the Xposed module instance (this module)
     * @param param Information about the process in which the module is loaded
     */
    public Lsp100HookEntry(@NonNull XposedModule self, @NonNull XposedModule.ModuleLoadedParam param) {
        this.self = self;
        mModule = param;
        Lsp100HookImpl.init(self);
    }

    @XposedApiExact(100)
    public void onPackageLoaded(@NonNull XposedModule.PackageLoadedParam param) {
        EzXHelperInit.INSTANCE.initHandleLoadPackageLsp10x(param);
        StartupInfo.setHookBridge(Lsp100HookImpl.INSTANCE);
        StartupInfo.setLoaderService(Lsp100HookImpl.INSTANCE);

        HookEntry.INSTANCE.handleLoadPackage(param.getPackageName());
    }

}
