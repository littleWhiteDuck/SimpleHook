package me.simpleHook.platform.hook.extension

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.AlgorithmRecordFilter
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper.writeWithCap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object MessageDigestHook : BaseHook() {
    private val digestBuffers = ConcurrentHashMap<MessageDigest, ByteArrayOutputStream>()
    private val updateByteBufferInput = ThreadLocal<ByteArray?>()

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.messageDigest) return
        XposedBridge.hookAllMethods(MessageDigest::class.java, "update", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val input = param.args.getOrNull(0) as? ByteBuffer
                updateByteBufferInput.set(input?.let { RecordOutHelper.captureBytes(it) })
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val md = param.thisObject as? MessageDigest ?: return
                    val algorithm = md.algorithm ?: "UNKNOWN"
                    if (!AlgorithmRecordFilter.shouldRecordDigest(
                            algorithm,
                            extensionConfig.algorithmConfig.messageDigestOptions
                        )
                    ) {
                        digestBuffers.remove(md)
                        return
                    }
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
                            val offset = (param.args[1] as? Int) ?: return
                            val len = (param.args[2] as? Int) ?: return
                            if (offset >= 0 && len >= 0 && offset + len <= input.size) {
                                writeWithCap(buf, input, offset, len)
                            }
                        }

                        param.args.size == 1 && param.args[0] is ByteBuffer -> {
                            updateByteBufferInput.get()?.let { writeWithCap(buf, it) }
                        }
                    }
                } finally {
                    updateByteBufferInput.remove()
                }
            }
        })

        XposedBridge.hookAllMethods(MessageDigest::class.java, "digest", object : XC_MethodHook() {

            override fun afterHookedMethod(param: MethodHookParam) {
                val md = param.thisObject as? MessageDigest ?: return
                val baos = digestBuffers.remove(md)
                val algorithm = md.algorithm ?: "UNKNOWN"
                if (!AlgorithmRecordFilter.shouldRecordDigest(
                        algorithm,
                        extensionConfig.algorithmConfig.messageDigestOptions
                    )
                ) {
                    return
                }

                // It seems that the `update(byte[] input)` method can't be hooked
                // when the call comes from `digest(byte[] input)`
                val rawBytes = if (param.args.size == 1 && param.args[0] is ByteArray) {
                    RecordOutHelper.captureBytes(param.args[0] as ByteArray)
                } else {
                    baos?.toByteArray() ?: ByteArray(0)
                }

                val resultBytes: ByteArray? = when {
                    param.args.isEmpty() -> param.result as? ByteArray
                    param.args.size == 1 -> param.result as? ByteArray
                    param.args.size == 3 -> {
                        val buf = param.args[0] as ByteArray
                        val off = param.args[1] as Int
                        val written = param.result as? Int
                        if (written != null && written >= 0 && off >= 0 && off + written <= buf.size) {
                            buf.copyOfRange(
                                off,
                                off + written
                            )
                        } else {
                            null
                        }
                    }

                    else -> null
                }

                RecordOutHelper.outputMac(
                    algorithm = algorithm,
                    rawData = rawBytes,
                    resultData = resultBytes ?: byteArrayOf()
                )
            }
        })

        XposedBridge.hookAllMethods(MessageDigest::class.java, "reset", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val md = param.thisObject as? MessageDigest ?: return
                digestBuffers.remove(md)
            }
        })
    }

}
