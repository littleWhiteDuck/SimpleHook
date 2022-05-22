package me.simpleHook.hook.extension

import android.content.Context
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import me.simpleHook.constant.Constant
import me.simpleHook.hook.BaseHook
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.log
import me.simpleHook.util.tip
import java.io.File

class HotFixHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {

    override fun startHook(packageName: String, strConfig: String) {
        val dexFilePaths: MutableList<String> = mutableListOf()
        val pathName = if (FlavorUtils.isNormal()) {
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/dex/"
        } else {
            Constant.CONFIG_MAIN_DIRECTORY + packageName + "/dex/"
        }
        val fileTree: FileTreeWalk = File(pathName).walk()
        fileTree.maxDepth(1).filter { it.isFile && it.extension == "dex" }.forEach {//循环处理符合条件的文件
            dexFilePaths.add(it.absolutePath)
        }
        try {
            for (index in 0 until dexFilePaths.size) {
                dexFilePaths[index].log(packageName)
                val originalLoader = mClassLoader
                val classLoader = DexClassLoader(
                    dexFilePaths[index], mContext.cacheDir.path, null, null
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

                val oldLength = java.lang.reflect.Array.getLength(originalDexElementsObject)
                val newLength = java.lang.reflect.Array.getLength(dexElementsObject)
                val concatDexElementsObject = java.lang.reflect.Array.newInstance(
                    dexElementsObject.javaClass.componentType, oldLength + newLength
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
            "hot fix error".tip(packageName)
        }
    }
}