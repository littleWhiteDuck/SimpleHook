package me.simpleHook.platform.hook.extension


import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.platform.hook.utils.AlgorithmRecordFilter
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
    private val updateByteBufferInput = ThreadLocal<ByteArray?>()

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.hmac) return
        XposedBridge.hookAllMethods(Mac::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as? Mac ?: return
                val algorithm = mac.algorithm ?: "unknown"
                if (!AlgorithmRecordFilter.shouldRecordHmac(
                        algorithm,
                        extensionConfig.algorithmConfig.hmacOptions
                    )
                ) {
                    macContexts.remove(mac)
                    return
                }
                val ctx = MacContext(algorithmType = algorithm)

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
                macContexts[mac] = ctx
            }
        })

        XposedBridge.hookAllMethods(
            Mac::class.java,
            "update",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val input = param.args.getOrNull(0) as? ByteBuffer
                    updateByteBufferInput.set(input?.let { RecordOutHelper.captureBytes(it) })
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val mac = param.thisObject as? Mac ?: return
                        val algorithm = macContexts[mac]?.algorithmType ?: mac.algorithm ?: "unknown"
                        if (!AlgorithmRecordFilter.shouldRecordHmac(
                                algorithm,
                                extensionConfig.algorithmConfig.hmacOptions
                            )
                        ) {
                            macContexts.remove(mac)
                            return
                        }
                        val ctx = macContexts.computeIfAbsent(mac) { MacContext(algorithmType = algorithm) }
                        ctx.algorithmType = algorithm
                        when (param.args.size) {
                            1 -> {
                                val p0 = param.args[0]
                                when (p0) {
                                    is Byte -> writeWithCap(ctx.dataStream, byteArrayOf(p0))
                                    is ByteArray -> writeWithCap(ctx.dataStream, p0, 0, p0.size)
                                    is ByteBuffer -> updateByteBufferInput.get()?.let {
                                        writeWithCap(ctx.dataStream, it)
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
                    } finally {
                        updateByteBufferInput.remove()
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
                    val algorithm = ctx.algorithmType ?: mac.algorithm ?: "unknown"
                    if (!AlgorithmRecordFilter.shouldRecordHmac(
                            algorithm,
                            extensionConfig.algorithmConfig.hmacOptions
                        )
                    ) {
                        return
                    }

                    // doFinal(byte[]) consumes extra input. doFinal(byte[], int) writes into caller output.
                    if (param.args.size == 1 && param.args[0] is ByteArray) {
                        val arr = param.args[0] as ByteArray
                        writeWithCap(ctx.dataStream, arr, 0, arr.size)
                    }

                    val result = when {
                        param.result is ByteArray -> param.result as ByteArray
                        param.args.size == 2 && param.args[0] is ByteArray -> {
                            val output = param.args[0] as ByteArray
                            val offset = param.args[1] as? Int
                            val macLength = runCatching { mac.macLength }.getOrDefault(0)
                            copyOutputBytes(output, offset, macLength)
                        }

                        else -> null
                    }

                    RecordOutHelper.outputHmac(
                        algorithm = algorithm,
                        key = ctx.keyBase,
                        rawData = ctx.dataStream.toByteArray(),
                        resultData = result ?: byteArrayOf()
                    )
                }
            })

        XposedBridge.hookAllMethods(Mac::class.java, "reset", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as? Mac ?: return
                macContexts[mac]?.dataStream?.reset()
            }
        })
    }

    private fun copyOutputBytes(output: ByteArray, offset: Int?, written: Int): ByteArray? {
        val safeOffset = offset ?: return null
        if (written < 0 || safeOffset < 0 || safeOffset + written > output.size) return null
        return RecordOutHelper.captureBytes(output, safeOffset, written)
    }
}
