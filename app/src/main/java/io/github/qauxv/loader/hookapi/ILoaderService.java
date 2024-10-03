package io.github.qauxv.loader.hookapi;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Keep
public interface ILoaderService {

    @NonNull
    String getEntryPointName();

    @NonNull
    String getLoaderVersionName();

    int getLoaderVersionCode();

    void log(@NonNull String msg);

    void log(@NonNull Throwable tr);

    /**
     * Get the class loader helper used by the current underlying loader.
     *
     * @return null as default, if not set
     */
    @Nullable
    IClassLoaderHelper getClassLoaderHelper();

    /**
     * Set the class loader helper for the current implementation. You need to set one before doing something crazy.
     * <p>
     * The class loader helper is provided by the superstructure, and is used by the underlying loader to create class loaders.
     *
     * @param helper The class loader helper instance
     */
    void setClassLoaderHelper(@Nullable IClassLoaderHelper helper);

}
