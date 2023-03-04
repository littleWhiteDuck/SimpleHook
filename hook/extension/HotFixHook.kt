package me.simpleHook.hook.extension


import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.constant.Constant
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.util.FlavorUtils
import me.simpleHook.extension.log
import me.simpleHook.extension.tip
import java.io.File

object HotFixHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.hotFix) return
        val dexFilePaths: MutableList<String> = mutableListOf()
        val pathName = if (FlavorUtils.normalVersion) {
            Constant.ANDROID_DATA_PATH + HookHelper.hostPackageName + "/simpleHook/dex/"
        } else {
            Constant.ROOT_CONFIG_MAIN_DIRECTORY + HookHelper.hostPackageName + "/dex/"
        }
        val fileTree: FileTreeWalk = File(pathName).walk()
        fileTree.maxDepth(1).filter { it.isFile && it.extension == "dex" }.forEach {
            dexFilePaths.add(it.absolutePath)
        }
        try {
            for (index in 0 until dexFilePaths.size) {
                dexFilePaths[index].log(HookHelper.hostPackageName)
                val originalLoader = HookHelper.appClassLoader
                val classLoader = DexClassLoader(
                    dexFilePaths[index], HookHelper.appContext.cacheDir.path, null, null
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

                val oldLength = java.lang.reflect.Array.getLength(originalDexElementsObject!!)
                val newLength = java.lang.reflect.Array.getLength(dexElementsObject!!)
                val concatDexElementsObject =
                    java.lang.reflect.Array.newInstance(dexElementsObject.javaClass.componentType!!,
                        oldLength + newLength)
                for (i in 0 until newLength) {
                    java.lang.reflect.Array.set(concatDexElementsObject,
                        i,
                        java.lang.reflect.Array.get(dexElementsObject, i))
                }
                for (i in 0 until oldLength) {
                    java.lang.reflect.Array.set(concatDexElementsObject,
                        newLength + i,
                        java.lang.reflect.Array.get(originalDexElementsObject, i))
                }
                dexElementsField[originalPathListObject] = concatDexElementsObject
            }

        } catch (e: Throwable) {
            "hot fix error".tip(HookHelper.hostPackageName)
        }
    }
}