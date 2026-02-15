package me.simpleHook.platform.hook.extension


import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper.recordValue
import me.simpleHook.platform.hook.utils.RecordOutHelper.writeWithCap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.Key
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacHook : BaseHook() {
    private data class MacContext(
        var keyBase: Map<RecordValueType, String>? = null,
        var keyAlgorithm: String? = null,
        var algorithmType: String? = null,
        val dataStream: ByteArrayOutputStream = ByteArrayOutputStream(256)
    )

    private val macContexts = ConcurrentHashMap<Mac, MacContext>()
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.hmac) return
        XposedBridge.hookAllMethods(Mac::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as? Mac ?: return
                val ctx = macContexts.computeIfAbsent(mac) { MacContext() }

                val keyArg = param.args.getOrNull(0)
                if (keyArg is SecretKeySpec) {
                    try {
                        ctx.keyBase = keyArg.encoded?.recordValue
                    } catch (_: Throwable) {
                    }
                    ctx.keyAlgorithm = keyArg.algorithm
                } else if (keyArg is Key) {
                    try {
                        ctx.keyBase = keyArg.encoded?.recordValue
                    } catch (_: Throwable) {
                    }
                    ctx.keyAlgorithm = keyArg.algorithm
                }
                ctx.algorithmType = mac.algorithm
            }
        })

        XposedBridge.hookAllMethods(
            Mac::class.java,
            "update",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mac = param.thisObject as? Mac ?: return
                    val ctx = macContexts.computeIfAbsent(mac) { MacContext() }
                    when (param.args.size) {
                        1 -> {
                            val p0 = param.args[0]
                            when (p0) {
                                is Byte -> writeWithCap(ctx.dataStream, byteArrayOf(p0))
                                is ByteArray -> writeWithCap(ctx.dataStream, p0, 0, p0.size)
                                is ByteBuffer -> {
                                    val dup = p0.duplicate()
                                    val tmp = ByteArray(dup.remaining())
                                    dup.get(tmp)
                                    writeWithCap(ctx.dataStream, tmp, 0, tmp.size)
                                }
                            }
                        }

                        3 -> {
                            val input = param.args[0] as? ByteArray ?: return
                            val off = (param.args[1] as? Int) ?: return
                            val len = (param.args[2] as? Int) ?: return
                            if (off >= 0 && len >= 0 && off + len <= input.size) {
                                writeWithCap(ctx.dataStream, input, off, len)
                            }
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            Mac::class.java,
            "doFinal",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mac = param.thisObject as? Mac ?: return
                    val ctx = macContexts.remove(mac) ?: MacContext()
                    ctx.algorithmType = ctx.algorithmType ?: mac.algorithm

                    // doFinal can have 0 or 1 args (sometimes doFinal(byte[]))
                    if (param.args.size == 1 && param.args[0] is ByteArray) {
                        val arr = param.args[0] as ByteArray
                        writeWithCap(ctx.dataStream, arr, 0, arr.size)
                    }

                    val result = param.result as? ByteArray

                    RecordOutHelper.outputHmac(
                        algorithm = ctx.algorithmType ?: "unknown",
                        key = ctx.keyBase,
                        rawData = ctx.dataStream.toByteArray(),
                        resultData = result ?: byteArrayOf()
                    )
                    try {
                        ctx.dataStream.reset()
                    } catch (_: Throwable) {
                    }
                }
            })
    }
}