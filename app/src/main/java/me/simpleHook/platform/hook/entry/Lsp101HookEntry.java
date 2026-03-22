/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.simpleHook.platform.hook.entry;

import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.XposedApiMin;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import io.github.qauxv.loader.sbl.lsp101.Lsp101HookImpl;
import io.github.qauxv.poststartup.StartupInfo;

/**
 * Entry point for libxpsoed API 101 (typically LSPosed).
 * <p>
 * The libxpsoed API is used as ART hook implementation.
 */
@RequiresApi(26)
@XposedApiMin(101)
public class Lsp101HookEntry implements Lsp10xHookEntryHandler {

    private final XposedModule self;

    public Lsp101HookEntry(@NonNull XposedModule self) {
        // do nothing according to libxposed API 101 specification,
        // initialization code should be placed after onModuleLoaded is called,
        // and the constructor should not do any initialization work.
        this.self = self;
    }

    public void onModuleLoaded(@NonNull XposedModule.ModuleLoadedParam param) {
        Lsp101HookImpl.init(self);
    }

    public void onPackageLoaded(@NonNull XposedModule.PackageLoadedParam param) {
        // not interested in this call
    }

    public void onPackageReady(@NonNull XposedModule.PackageReadyParam param) {
        EzXHelperInit.INSTANCE.initHandleLoadPackageLsp10x(param);
        StartupInfo.setHookBridge(Lsp101HookImpl.INSTANCE);
        StartupInfo.setLoaderService(Lsp101HookImpl.INSTANCE);

        HookEntry.INSTANCE.handleLoadPackage(param.getPackageName());
    }

}
