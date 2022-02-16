package me.simpleHook.hook

import android.content.Context
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import me.simpleHook.constant.Constant
import me.simpleHook.util.tip
import java.io.File

object HotFix {
    fun startFix(context: Context?, packageName: String) {
        if (context == null) return
        val dexFilePaths: MutableList<String> = mutableListOf()
        val fileTree: FileTreeWalk =
            File(Constant.ANDROID_DATA_PATH + packageName + "simpleHook/" + "dex/").walk()
        fileTree.maxDepth(1)//遍历目录层级为1，即无需检查子目录
            .filter { it.isFile } //只挑选出文件,不处理文件夹
            .filter { it.extension == "dex" } //选择扩展名为“png”的处理
            .forEach {//循环处理符合条件的文件
                dexFilePaths.add(it.absolutePath)
            }
        try {
            for (index in 0 until dexFilePaths.size) {
                val originalLoader = context.classLoader
                val classLoader = DexClassLoader(
                    dexFilePaths[index],
                    context.cacheDir.path,
                    null,
                    null
                )
                val loaderClass: Class<*> = BaseDexClassLoader::class.java
                val pathListField = loaderClass.getDeclaredField("pathList")
                pathListField.isAccessible = true
                val pathListObject = pathListField[classLoader]
                val pathListClass: Class<*> = pathListObject.javaClass
                val dexElementsField = pathListClass.getDeclaredField("dexElements")
                dexElementsField.isAccessible = true
                val dexElementsObject = dexElementsField[pathListObject]
                val originalPathListObject = pathListField[originalLoader]
                val originalDexElementsObject = dexElementsField[originalPathListObject]

                //数组操作，把最新的补丁dex文件插入到最前面
                val oldLength = java.lang.reflect.Array.getLength(originalDexElementsObject)
                val newLength = java.lang.reflect.Array.getLength(dexElementsObject)
                val concatDexElementsObject = java.lang.reflect.Array.newInstance(
                    dexElementsObject.javaClass.componentType,
                    oldLength + newLength
                )
                for (i in 0 until newLength) {
                    java.lang.reflect.Array.set(
                        concatDexElementsObject,
                        i,
                        java.lang.reflect.Array.get(dexElementsObject, i)
                    )
                }
                for (i in 0 until oldLength) {
                    java.lang.reflect.Array.set(
                        concatDexElementsObject,
                        newLength + i,
                        java.lang.reflect.Array.get(originalDexElementsObject, i)
                    )
                }
                dexElementsField[originalPathListObject] = concatDexElementsObject
            }

        } catch (e: Exception) {
            "hot fix error".tip()
        }
    }
    /*
      public static void startHotFix(Context context, ClassLoader originalClassLoader, String dexPath) {
        try {
            DexClassLoader classLoader = new DexClassLoader(dexPath,
                    context.getCacheDir().getPath(), null, null);
            Class loaderClass = BaseDexClassLoader.class;
            Field pathListField = loaderClass.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathListObject = pathListField.get(classLoader);
            Class pathListClass = pathListObject.getClass();
            Field dexElementsField = pathListClass.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object dexElementsObject = dexElementsField.get(pathListObject);
            Object originalPathListObject = pathListField.get(originalClassLoader);
            Object originalDexElementsObject = dexElementsField.get(originalPathListObject);
            int oldLength = Array.getLength(originalDexElementsObject);
            int newLength = Array.getLength(dexElementsObject);
            Object concatDexElementsObject = Array.newInstance(dexElementsObject.getClass().getComponentType(), oldLength + newLength);
            for (int i = 0; i < newLength; i++) {
                Array.set(concatDexElementsObject, i, Array.get(dexElementsObject, i));
            }
            for (int i = 0; i < oldLength; i++) {
                Array.set(concatDexElementsObject, newLength + i, Array.get(originalDexElementsObject, i));
            }
            dexElementsField.set(originalPathListObject, concatDexElementsObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
     */
}