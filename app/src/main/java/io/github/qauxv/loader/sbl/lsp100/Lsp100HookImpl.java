package io.github.qauxv.loader.sbl.lsp100;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.qauxv.loader.hookapi.IClassLoaderHelper;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import me.simpleHook.BuildConfig;

public class Lsp100HookImpl implements IHookBridge, ILoaderService {

    public static final Lsp100HookImpl INSTANCE = new Lsp100HookImpl();
    public static XposedModule self = null;
    private IClassLoaderHelper mClassLoaderHelper;

    private Lsp100HookImpl() {
    }

    public static void init(@NonNull XposedModule base) {
        self = base;
        Lsp100HookWrapper.self = base;
    }

    @Override
    public int getApiLevel() {
        return XposedInterface.API;
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return self.getFrameworkName();
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return self.getFrameworkVersion();
    }

    @Override
    public long getFrameworkVersionCode() {
        return self.getFrameworkVersionCode();
    }

    @NonNull
    @Override
    public MemberUnhookHandle hookMethod(@NonNull Member member, @NonNull IMemberHookCallback callback, int priority) {
        return Lsp100HookWrapper.hookAndRegisterMethodCallback(member, callback, priority);
    }

    @Override
    public boolean isDeoptimizationSupported() {
        return true;
    }

    @Override
    public boolean deoptimize(@NonNull Member member) {
        CheckUtils.checkNonNull(member, "member");
        if (member instanceof Method) {
            return self.deoptimize((Method) member);
        } else if (member instanceof Constructor) {
            return self.deoptimize((Constructor<?>) member);
        } else {
            throw new IllegalArgumentException("only method and constructor can be deoptimized");
        }
    }

    @Nullable
    @Override
    public Object invokeOriginalMethod(@NonNull Method method, @Nullable Object thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(args, "args");
        return self.invokeOrigin(method, thisObject, args);
    }

    @NonNull
    @Override
    public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, @NonNull Object... args)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        CheckUtils.checkNonNull(constructor, "constructor");
        CheckUtils.checkNonNull(args, "args");
        return self.newInstanceOrigin(constructor, args);
    }

    @Override
    public <T> void invokeOriginalConstructor(@NonNull Constructor<T> ctor, @NonNull T thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        CheckUtils.checkNonNull(ctor, "ctor");
        CheckUtils.checkNonNull(thisObject, "thisObject");
        CheckUtils.checkNonNull(args, "args");
        self.invokeOrigin(ctor, thisObject, args);
    }

    @NonNull
    @Override
    public String getEntryPointName() {
        return this.getClass().getName();
    }

    @NonNull
    @Override
    public String getLoaderVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public int getLoaderVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public void log(@NonNull String msg) {
        self.log(msg);
    }

    @Override
    public void log(@NonNull Throwable tr) {
        self.log(tr.toString(), tr);
    }

    @Override
    public void setClassLoaderHelper(@Nullable IClassLoaderHelper helper) {
        mClassLoaderHelper = helper;
    }

    @Nullable
    @Override
    public IClassLoaderHelper getClassLoaderHelper() {
        return mClassLoaderHelper;
    }

    @Override
    public long getHookCounter() {
        return Lsp100HookWrapper.getHookCounter();
    }

}
