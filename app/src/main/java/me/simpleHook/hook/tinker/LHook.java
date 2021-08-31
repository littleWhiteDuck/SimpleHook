package me.simpleHook.hook.tinker;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import dalvik.system.DexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Lyh on
 * 2019/11/18
 */
public class LHook implements IXposedHookLoadPackage {

    private static String TAG = "lyh222222222";


    private static String PackageName = "com.example.xposedtinker";

    private Context mContext;
    public  static   ClassLoader mLoader;

    private static String ClassPath = "com.example.xposedtinker.MainActivity";

    private static String MoethodName = "Test";

    private static Class<?> bin = null;

    private DexClassLoader mDexClassLoader;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(lpparam.packageName.equals("me.hookTest")){
            HookOnAttach();
        }
        XposedBridge.log("ddddddddddddddddd");
    }

    private void HookOnAttach() {
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                mContext = (Context) param.args[0];
                mLoader = mContext.getClassLoader();
                Log.e(TAG, "拿到classloader ");
                HookMain();
            }
        });
    }
    /**
     *  先用 自己的 classloader去加载 dex
     */
    private void initMyClassloader() {
        CLogUtils.e("开始 动态 加载 初始化 ");

        File dexOutputDir = mContext.getDir("dex", 0);

        //CLogUtils.e("dexOutputDir  dex  666 " + dexOutputDir.getAbsolutePath());
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器
        mDexClassLoader = new DexClassLoader("/storage/emulated/0/dex/1.dex", dexOutputDir.getAbsolutePath(), null, mLoader);

    }



    /**
     * 将 Elements 数组 set回原来的 classloader里面
     * @param dexElementsResut
     */
    private boolean SetDexElements(Object[] dexElementsResut,int conunt) {
        try {
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    //先 重新设置一次
                    dexElementsField.set(dexPathList,dexElementsResut);
                    //重新 get 用
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if(dexElements.length==conunt&& Arrays.hashCode(dexElements) == Arrays.hashCode(dexElementsResut)){
                        CLogUtils.e("替换 以后的 长度 是 "+dexElements.length);
                        return true;
                    }else {
                        CLogUtils.e("合成   长度  "+dexElements.length+"传入 数组 长度   "+conunt);

                        CLogUtils.e("   dexElements hashCode "+Arrays.hashCode(dexElements)+"  "+Arrays.hashCode(dexElementsResut));

                        return false;
                    }
                }else {
                    CLogUtils.e("SetDexElements  获取 dexElements == null");
                }
            }else {
                CLogUtils.e("SetDexElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("SetDexElements  NoSuchFieldException   "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("SetDexElements  IllegalAccessException   "+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    private void HookMain() {
        //先初始化 装载 正确的Test方法 的  classaloader
        initMyClassloader();
        //先拿到 自定义 加载的 classloader
        Object[] MyDexClassloader = getClassLoaderElements(mDexClassLoader);
        //拿到当前进程的 classloader
        Object[] otherClassloader = getClassLoaderElements(mLoader);
        if(otherClassloader!=null&&MyDexClassloader!=null) {
            //把两个 数组 DexElements合并 把自己正确的 dex放在前面
            // 这样就可以 在需要的时候 先拿到 我们自己定义的classloader
            // 首先开辟一个 新的 数组 大小 是前里个大小的 和
            Object[] combined = (Object[]) Array.newInstance(otherClassloader.getClass().getComponentType(),
                    MyDexClassloader.length + otherClassloader.length);
            //将自己classloader 数组的内容 放到 前面位置
            System.arraycopy(MyDexClassloader, 0, combined, 0, MyDexClassloader.length);
            //把 原来的 进行 拼接
            System.arraycopy(otherClassloader, 0, combined, MyDexClassloader.length, otherClassloader.length);
            //判断 是否合并 成功
            if ((MyDexClassloader.length + otherClassloader.length) != combined.length) {
                CLogUtils.e("合并 elements数组 失败  null");
            }
            //将 生成的 classloader进行 set回原来的 element数组
            if(SetDexElements(combined,MyDexClassloader.length + otherClassloader.length)){
                CLogUtils.e("替换成功");
            }else {
                CLogUtils.e("替换失败 ");
            }
        }else {
            CLogUtils.e("没有 拿到 classloader");
        }

    }



    private Object[] getClassLoaderElements(ClassLoader classLoader)  {
        try {
            Field pathListField = classLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(classLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if(dexElements!=null){
                        return dexElements;
                    }else {
                        CLogUtils.e("AddElements  获取 dexElements == null");
                    }
                    //ArrayUtils.addAll(first, second);
                }else {
                    CLogUtils.e("AddElements  获取 dexElements == null");
                }
            }else {
                CLogUtils.e("AddElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("AddElements  NoSuchFieldException   "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("AddElements  IllegalAccessException   "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
