package me.simpleHook.platform.hook.extension


import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.xLog
import me.simpleHook.core.utils.FlavorUtil
import java.io.File

object HotFixHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.hotFix) return

        val pathName = if (FlavorUtil.normalVersion) {
            ConfigConstant.NORMAL_DEX_PATH.format(HookHelper.hostPackageName)
        } else {
            ConfigConstant.ROOT_DEX_PATH.format(HookHelper.hostPackageName)
        }

        val dexFilePaths = File(pathName)
            .walk()
            .maxDepth(1)
            .filter { it.isFile && it.extension == "dex" }
            .map { it.absolutePath }
            .toList()

        try {
            for (index in 0 until dexFilePaths.size) {
                dexFilePaths[index].xLog()
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
                    java.lang.reflect.Array.newInstance(
                        dexElementsObject.javaClass.componentType!!,
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

        } catch (_: Throwable) {
            "hot fix error".xLog()
        }
    }
}