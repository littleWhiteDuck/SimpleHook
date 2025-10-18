package me.simpleHook.hook.extension

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.hook.utils.RecordOutHelper
import me.simpleHook.hook.utils.RecordOutHelper.writeWithCap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object MessageDigestHook : BaseHook() {
    private val digestBuffers = ConcurrentHashMap<MessageDigest, ByteArrayOutputStream>()

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.digest) return
        XposedBridge.hookAllMethods(MessageDigest::class.java, "update", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val md = param.thisObject as? MessageDigest ?: return
                val buf = digestBuffers.computeIfAbsent(md) { ByteArrayOutputStream(256) }

                when {
                    param.args.size == 1 && param.args[0] is Byte -> {
                        val b = (param.args[0] as Byte)
                        writeWithCap(buf, byteArrayOf(b))
                    }

                    param.args.size == 1 && param.args[0] is ByteArray -> {
                        val data = param.args[0] as ByteArray
                        writeWithCap(buf, data, 0, data.size)
                    }

                    param.args.size == 3 && param.args[0] is ByteArray -> {
                        val input = param.args[0] as ByteArray
                        val offset = (param.args[1] as Int)
                        val len = (param.args[2] as Int)
                        if (offset >= 0 && len >= 0 && offset + len <= input.size) {
                            writeWithCap(buf, input, offset, len)
                        }
                    }

                    param.args.size == 1 && param.args[0] is ByteBuffer -> {
                        val bb = param.args[0] as ByteBuffer
                        val dup = bb.duplicate()
                        val tmp = ByteArray(dup.remaining())
                        dup.get(tmp)
                        writeWithCap(buf, tmp)
                    }
                }
            }
        })

        XposedBridge.hookAllMethods(MessageDigest::class.java, "digest", object : XC_MethodHook() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun afterHookedMethod(param: MethodHookParam) {
                val md = param.thisObject as? MessageDigest ?: return
                val baos = digestBuffers.remove(md)
                val rawBytes = baos?.toByteArray() ?: ByteArray(0)

                val resultBytes: ByteArray? = when {
                    param.args.isEmpty() && param.result is ByteArray -> param.result as ByteArray
                    param.args.size == 1 && param.args[0] is ByteArray && param.result is ByteArray -> param.result as ByteArray
                    param.args.size == 3 && param.args[0] is ByteArray && param.result is Int -> {
                        val buf = param.args[0] as ByteArray
                        val off = param.args[1] as Int
                        val written = param.result as Int
                        if (written > 0 && off >= 0 && off + written <= buf.size) buf.copyOfRange(
                            off,
                            off + written
                        ) else null
                    }

                    else -> if (param.result is ByteArray) param.result as ByteArray else null
                }

                val algorithm = md.algorithm ?: "UNKNOWN"

                RecordOutHelper.outputHmac(
                    algorithm = algorithm,
                    rawData = rawBytes,
                    resultData = resultBytes ?: byteArrayOf()
                )
            }
        })
    }

}