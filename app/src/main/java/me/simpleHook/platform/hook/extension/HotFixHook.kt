package me.simpleHook.platform.hook.extension


import android.annotation.SuppressLint
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.xLog
import java.io.File

object HotFixHook : BaseHook() {

    @SuppressLint("DiscouragedPrivateApi")
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.hotFix) return

        val sourceDexDir = File(ConfigConstant.ROOT_DEX_PATH.format(HookHelper.hostPackageName))
        val sourceDexFiles = sourceDexDir
            .walk()
            .maxDepth(1)
            .filter { it.isFile && it.extension.equals("dex", ignoreCase = true) }
            .sortedBy { it.name }
            .toList()
        if (sourceDexFiles.isEmpty()) return

        try {
            val readOnlyDexFiles = prepareReadOnlyDexFiles(sourceDexFiles)
            if (readOnlyDexFiles.isEmpty()) return
            val originalLoader = HookHelper.appClassLoader
            for (dexFile in readOnlyDexFiles) {
                dexFile.absolutePath.xLog()
                val classLoader = DexClassLoader(
                    dexFile.absolutePath,
                    HookHelper.appContext.codeCacheDir.path,
                    null,
                    originalLoader
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

        } catch (e: Throwable) {
            "hot fix error: ${e.message}".xLog()
        }
    }

    private fun prepareReadOnlyDexFiles(sourceDexFiles: List<File>): List<File> {
        val hotFixDir = File(HookHelper.appContext.codeCacheDir, "simplehook_hotfix")
        if (hotFixDir.exists()) {
            hotFixDir.deleteRecursively()
        }
        if (!hotFixDir.exists() && !hotFixDir.mkdirs()) {
            "hot fix error: cannot create dir ${hotFixDir.absolutePath}".xLog()
            return emptyList()
        }

        return sourceDexFiles.mapIndexedNotNull { index, sourceFile ->
            runCatching {
                val targetFile = File(hotFixDir, "${index}_${sourceFile.name}")
                sourceFile.copyTo(targetFile, overwrite = true)
                applyReadOnly(targetFile)
                if (targetFile.canWrite()) {
                    "hot fix error: dex is still writable ${targetFile.absolutePath}".xLog()
                    null
                } else {
                    targetFile
                }
            }.getOrElse { throwable ->
                "hot fix error: copy dex failed ${sourceFile.absolutePath}, ${throwable.message}".xLog()
                null
            }
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun applyReadOnly(file: File) {
        file.setReadable(true, false)
        file.setWritable(false, false)
        if (file.canWrite()) {
            file.setReadOnly()
        }
    }
}
