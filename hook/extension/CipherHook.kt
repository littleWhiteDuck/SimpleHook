package me.simpleHook.platform.hook.extension

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordCipher
import me.simpleHook.data.record.RecordCipherMode
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper.recordValue
import me.simpleHook.platform.hook.utils.RecordOutHelper.writeWithCap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.Key
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object CipherHook : BaseHook() {
    private data class CipherContext(
        var keyBase: Map<RecordValueType, String>? = null,
        var keyAlgorithm: String? = null,
        var iv: Map<RecordValueType, String>? = null,
        var cryptType: RecordCipherMode = RecordCipherMode.Unknown,
        var algorithmType: String? = null,
        val dataStream: ByteArrayOutputStream = ByteArrayOutputStream(256)
    )

    private val cipherContexts = ConcurrentHashMap<Cipher, CipherContext>()

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.cipher) return

        XposedBridge.hookAllMethods(Cipher::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val ctx = cipherContexts.computeIfAbsent(cipher) { CipherContext() }

                // opmode is first arg; key commonly next
                val opmode = param.args.getOrNull(0) as? Int
                val cryptType = when (opmode) {
                    Cipher.ENCRYPT_MODE -> RecordCipherMode.Encrypt
                    Cipher.DECRYPT_MODE -> RecordCipherMode.Decrypt
                    Cipher.WRAP_MODE -> RecordCipherMode.Wrap
                    Cipher.UNWRAP_MODE -> RecordCipherMode.Unwrap
                    else -> RecordCipherMode.Unknown
                }

                // key is usually arg 1
                val keyArg = param.args.getOrNull(1)
                if (keyArg is Key) {
                    ctx.keyAlgorithm = keyArg.algorithm
                    try {
                        val encoded = keyArg.encoded
                        if (encoded != null) {
                            ctx.keyBase = encoded.recordValue
                        }
                    } catch (_: Throwable) {
                        // some Keys may not expose encoded bytes
                    }
                }

                // sometimes iv is provided as arg 2 or contained in AlgorithmParameters/Spec
                val maybeIv = param.args.getOrNull(2)
                when (maybeIv) {
                    is IvParameterSpec -> ctx.iv = maybeIv.iv.recordValue

                    is java.security.AlgorithmParameters -> {
                        try {
                            val spec = maybeIv.getParameterSpec(IvParameterSpec::class.java)
                            ctx.iv = spec.iv.recordValue
                        } catch (_: Throwable) {
                        }
                    }

                    is javax.crypto.spec.GCMParameterSpec -> {
                        try {
                            ctx.iv = maybeIv.iv.recordValue
                        } catch (_: Throwable) {
                        }
                    }
                    // else ignore
                }

                ctx.algorithmType = cipher.algorithm ?: "unknown"
                ctx.cryptType = cryptType
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "update", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val ctx = cipherContexts.computeIfAbsent(cipher) { CipherContext() }
                when {
                    param.args.size == 1 -> {
                        val p0 = param.args[0]
                        when (p0) {
                            is ByteArray -> {
                                val data = p0
                                writeWithCap(ctx.dataStream, data, 0, data.size)
                            }

                            is Byte -> {
                                writeWithCap(ctx.dataStream, byteArrayOf(p0))
                            }

                            is ByteBuffer -> {
                                val dup = p0.duplicate()
                                val rem = dup.remaining()
                                val tmp = ByteArray(rem)
                                dup.get(tmp)
                                writeWithCap(ctx.dataStream, tmp, 0, tmp.size)
                            }
                        }
                    }

                    param.args.size == 3 -> {
                        val input = param.args[0] as? ByteArray ?: return
                        val offset = (param.args[1] as? Int) ?: return
                        val len = (param.args[2] as? Int) ?: return
                        if (offset >= 0 && len >= 0 && offset + len <= input.size) {
                            writeWithCap(ctx.dataStream, input, offset, len)
                        }
                    }
                }
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "doFinal", object : XC_MethodHook() {

            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val ctx = cipherContexts.remove(cipher) ?: CipherContext()
                ctx.algorithmType = ctx.algorithmType ?: cipher.algorithm

                // input args -> maybe raw data
                when {
                    param.args.isEmpty() -> { /* no extra */
                    }

                    param.args.size == 1 && param.args[0] is ByteArray -> {
                        val input = param.args[0] as ByteArray
                        writeWithCap(ctx.dataStream, input, 0, input.size)
                    }

                    param.args.size == 3 && param.args[0] is ByteArray -> {
                        val input = param.args[0] as ByteArray
                        val off = param.args[1] as? Int ?: return
                        val len = param.args[2] as? Int ?: return
                        if (off >= 0 && len >= 0 && off + len <= input.size) {
                            writeWithCap(ctx.dataStream, input, off, len)
                        }
                    }
                }

                val resultBytes = param.result as? ByteArray
                val algorithm = ctx.algorithmType ?: "unknown"

                RecordOutHelper.outputRecord(
                    type = RecordType.Cipher, subType = algorithm, record = RecordCipher(
                        algorithm = algorithm,
                        cryptType = ctx.cryptType,
                        key = ctx.keyBase,
                        iv = ctx.iv,
                        rawData = ctx.dataStream.toByteArray().recordValue,
                        resultData = resultBytes?.recordValue ?: emptyMap(),
                        stackDetail = RecordOutHelper.getStackTraceStr()
                    )
                )
                // clear buffer
                try {
                    ctx.dataStream.reset()
                } catch (_: Throwable) {
                }
            }
        })
    }
}