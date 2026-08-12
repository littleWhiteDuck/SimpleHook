package io.github.qauxv.poststartup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;

public class StartupInfo {

    private StartupInfo() {
        throw new AssertionError("No instance for you!");
    }

    private static ILoaderService loaderService;

    private static IHookBridge hookBridge;

    private static Boolean inHostProcess = null;

    @NonNull
    public static ILoaderService getLoaderService() {
        return loaderService;
    }

    @Nullable
    public static IHookBridge getHookBridge() {
        return hookBridge;
    }


    public static void setHookBridge(@Nullable IHookBridge hookBridge) {
        StartupInfo.hookBridge = hookBridge;
    }

    public static void setLoaderService(@NonNull ILoaderService loaderService) {
        Objects.requireNonNull(loaderService);
        StartupInfo.loaderService = loaderService;
    }

    public static boolean isInHostProcess() {
        if (inHostProcess == null) {
            throw new IllegalStateException("Host process status is not initialized");
        }
        return inHostProcess;
    }

    public static void setInHostProcess(boolean inHostProcess) {
        if (StartupInfo.inHostProcess != null) {
            throw new IllegalStateException("Host process status is already initialized");
        }
        StartupInfo.inHostProcess = inHostProcess;
    }

}
