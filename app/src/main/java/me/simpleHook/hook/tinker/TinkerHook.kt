package me.simpleHook.hook.tinker

import android.content.Context
import dalvik.system.DexClassLoader
import me.simpleHook.constant.Constant
import me.simpleHook.util.log

object TinkerHook {
    fun main(context: Context?, packageName: String) {
        context?.let { hookMain(it, packageName) }
    }

    private fun hookMain(context: Context, packageName: String) {
        //先拿到 自定义 加载的 classloader
        val myDexClassloader: Array<Any>? =
            getClassLoaderElements(initMyClassLoader(context, packageName))
        //拿到当前进程的 classloader
        //拿到当前进程的 classloader
        val otherClassloader: Array<Any>? = getClassLoaderElements(context.classLoader)
        if (otherClassloader != null && myDexClassloader != null) {
            //把两个 数组 DexElements合并 把自己正确的 dex放在前面
            // 这样就可以 在需要的时候 先拿到 我们自己定义的classloader
            // 首先开辟一个 新的 数组 大小 是前里个大小的 和
            val combined = java.lang.reflect.Array.newInstance(
                otherClassloader.javaClass.componentType,
                myDexClassloader.size + otherClassloader.size
            ) as Array<Any>
            //将自己classloader 数组的内容 放到 前面位置
            System.arraycopy(myDexClassloader, 0, combined, 0, myDexClassloader.size)
            //把 原来的 进行 拼接
            System.arraycopy(
                otherClassloader,
                0,
                combined,
                myDexClassloader.size,
                otherClassloader.size
            )
            //判断 是否合并 成功
            if (myDexClassloader.size + otherClassloader.size != combined.size) {
                "合并 elements数组 失败  null".log()
            }
            //将 生成的 classloader进行 set回原来的 element数组
            if (setDexElements(combined, myDexClassloader.size + otherClassloader.size, context)) {
                "替换成功".log()
            } else {
                "替换失败".log()
            }
        } else {
            "没有 拿到 classloader".log()
        }
    }

    private fun initMyClassLoader(context: Context, packageName: String): ClassLoader {
        return DexClassLoader(
            "${Constant.HOT_FIX_DIRECTORY + packageName}/tinker.dex",
            context.getDir("dex", Context.MODE_PRIVATE).absolutePath,
            null,
            context.classLoader
        )
    }

    private fun getClassLoaderElements(classLoader: ClassLoader): Array<Any>? {
        try {
            val pathListField = classLoader.javaClass.superclass.getDeclaredField("pathList")
            pathListField.isAccessible = true
            val dexPathList = pathListField[classLoader]
            val dexElementsField = dexPathList.javaClass.getDeclaredField("dexElements")
            dexElementsField.isAccessible = true
            return dexElementsField[dexPathList] as Array<Any>
            //ArrayUtils.addAll(first, second);
        } catch (e: NoSuchFieldException) {
           "AddElements  NoSuchFieldException".log()
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            "AddElements  IllegalAccessException".log()
            e.printStackTrace()
        }
        return null
    }

    private fun setDexElements(
        dexElementsResult: Array<Any>,
        count: Int,
        context: Context
    ): Boolean {
        try {
            val pathListField =
                context.classLoader.javaClass.superclass.getDeclaredField("pathList")
            pathListField.isAccessible = true
            val dexPathList = pathListField[context.classLoader]
            val dexElementsField = dexPathList.javaClass.getDeclaredField("dexElements")
            dexElementsField.isAccessible = true
            //先 重新设置一次
            dexElementsField[dexPathList] = dexElementsResult
            //重新 get 用
            val dexElements = dexElementsField[dexPathList] as Array<Any>
            return if (dexElements.size == count && dexElements.contentHashCode() == dexElementsResult.contentHashCode()
            ) {
                "替换 以后的 长度 是 ${dexElements.size}".log()
                true
            } else {
                "合成长度${dexElements.size}, 传入的数组长度$count".log()
                "dexElements hashCode ${dexElements.contentHashCode()}, ${dexElementsResult.contentHashCode()}"
                false
            }
        } catch (e: NoSuchFieldException) {
            "SetDexElements  NoSuchFieldException   ".log()
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            "SetDexElements  IllegalAccessException   ".log()
            e.printStackTrace()
        }
        return false
    }
}