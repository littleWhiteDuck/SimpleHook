package io.github.qauxv.loader.sbl.xp51;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Member;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;
import io.github.qauxv.loader.hookapi.IHookBridge;

public class Xp51HookWrapper {

    private static final AtomicLong sNextHookId = new AtomicLong(1);
    private static final String TAG_PREFIX = "simpleHook_hcb_";

    public static class Xp51HookParam implements IHookBridge.IMemberHookParam {

        private XC_MethodHook.MethodHookParam mParam;
        private Object mExtra;

        @NonNull
        @Override
        public Member getMember() {
            checkLifecycle();
            return mParam.method;
        }

        @Nullable
        @Override
        public Object getThisObject() {
            checkLifecycle();
            return mParam.thisObject;
        }

        @NonNull
        @Override
        public Object[] getArgs() {
            checkLifecycle();
            return mParam.args;
        }

        @Nullable
        @Override
        public Object getResult() {
            checkLifecycle();
            return mParam.getResult();
        }

        @Override
        public void setResult(@Nullable Object result) {
            checkLifecycle();
            mParam.setResult(result);
        }

        @Nullable
        @Override
        public Throwable getThrowable() {
            checkLifecycle();
            return mParam.getThrowable();
        }

        @Override
        public void setThrowable(@NonNull Throwable throwable) {
            checkLifecycle();
            mParam.setThrowable(throwable);
        }

        @Nullable
        @Override
        public Object getExtra() {
            checkLifecycle();
            return mExtra;
        }

        @Override
        public void setExtra(@Nullable Object extra) {
            checkLifecycle();
            mExtra = extra;
        }

        private void checkLifecycle() {
            if (mParam == null) {
                throw new IllegalStateException("attempt to access hook param after destroyed");
            }
        }
    }

    public static class Xp51HookCallback extends XC_MethodHook {

        private final IHookBridge.IMemberHookCallback mCallback;
        private final long mHookId = sNextHookId.getAndIncrement();
        private boolean mAlive = true;

        public Xp51HookCallback(@NonNull IHookBridge.IMemberHookCallback c, int priority) {
            super(priority);
            mCallback = c;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (!mAlive) {
                return;
            }
            String tag = TAG_PREFIX + mHookId;
            Xp51HookParam hcbParam = new Xp51HookParam();
            hcbParam.mParam = param;
            param.setObjectExtra(tag, hcbParam);
            mCallback.beforeHookedMember(hcbParam);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!mAlive) {
                return;
            }
            String tag = TAG_PREFIX + mHookId;
            Xp51HookParam hcbParam = (Xp51HookParam) param.getObjectExtra(tag);
            if (hcbParam == null) {
                throw new AssertionError("hcbParam is null, tag: " + tag);
            }
            mCallback.afterHookedMember(hcbParam);
            // for gc
            param.setObjectExtra(tag, null);
            hcbParam.mParam = null;
            hcbParam.mExtra = null;
        }
    }

    public static class Xp51UnhookHandle implements IHookBridge.MemberUnhookHandle {

        private final XC_MethodHook.Unhook mUnhook;
        private final Xp51HookCallback mCallback;
        private final Member mMember;

        public Xp51UnhookHandle(@NonNull XC_MethodHook.Unhook unhook, @NonNull Member member, @NonNull Xp51HookCallback callback) {
            mUnhook = unhook;
            mMember = member;
            mCallback = callback;
        }

        @NonNull
        @Override
        public Member getMember() {
            return mMember;
        }

        @NonNull
        @Override
        public IHookBridge.IMemberHookCallback getCallback() {
            return mCallback.mCallback;
        }

        @Override
        public boolean isHookActive() {
            return mCallback.mAlive;
        }

        @Override
        public void unhook() {
            mUnhook.unhook();
            mCallback.mAlive = false;
        }

    }

    public static int getHookCounter() {
        return (int) (sNextHookId.get() - 1);
    }

}
