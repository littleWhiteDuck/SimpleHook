package me.simpleHook.platform.hook.extension

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordCipher
import me.simpleHook.data.record.RecordCipherMode
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.platform.hook.utils.AlgorithmRecordFilter
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper.recordValue
import me.simpleHook.platform.hook.utils.RecordOutHelper.writeWithCap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.Key
import java.security.cert.Certificate
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
        val dataStream: ByteArrayOutputStream = ByteArrayOutputStream(256),
        val aadStream: ByteArrayOutputStream = ByteArrayOutputStream(256),
        val resultStream: ByteArrayOutputStream = ByteArrayOutputStream(256)
    )

    private val cipherContexts = ConcurrentHashMap<Cipher, CipherContext>()
    private val updateAadByteBufferInput = ThreadLocal<ByteArray?>()
    private val updateByteBufferInput = ThreadLocal<ByteArray?>()
    private val updateByteBufferOutputPosition = ThreadLocal<Int?>()
    private val doFinalByteBufferInput = ThreadLocal<ByteArray?>()
    private val doFinalByteBufferOutputPosition = ThreadLocal<Int?>()

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.cipher) return

        XposedBridge.hookAllMethods(Cipher::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val algorithm = cipher.algorithm ?: "unknown"
                if (!AlgorithmRecordFilter.shouldRecordCipher(
                        algorithm,
                        extensionConfig.algorithmConfig.cipherOptions
                    )
                ) {
                    cipherContexts.remove(cipher)
                    return
                }

                // opmode is first arg; key commonly next
                val opmode = param.args.getOrNull(0) as? Int
                val cryptType = when (opmode) {
                    Cipher.ENCRYPT_MODE -> RecordCipherMode.Encrypt
                    Cipher.DECRYPT_MODE -> RecordCipherMode.Decrypt
                    Cipher.WRAP_MODE -> RecordCipherMode.Wrap
                    Cipher.UNWRAP_MODE -> RecordCipherMode.Unwrap
                    else -> RecordCipherMode.Unknown
                }
                val ctx = CipherContext(algorithmType = algorithm, cryptType = cryptType)

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
                } else if (keyArg is Certificate) {
                    try {
                        val encoded = keyArg.publicKey?.encoded
                        if (encoded != null) {
                            ctx.keyBase = encoded.recordValue
                        }
                    } catch (_: Throwable) {
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

                cipherContexts[cipher] = ctx
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "updateAAD", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val input = param.args.getOrNull(0) as? ByteBuffer
                updateAadByteBufferInput.set(input?.let { RecordOutHelper.captureBytes(it) })
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val cipher = param.thisObject as? Cipher ?: return
                    val algorithm = cipherContexts[cipher]?.algorithmType ?: cipher.algorithm ?: "unknown"
                    if (!AlgorithmRecordFilter.shouldRecordCipher(
                            algorithm,
                            extensionConfig.algorithmConfig.cipherOptions
                        )
                    ) {
                        cipherContexts.remove(cipher)
                        return
                    }
                    val ctx = cipherContexts.computeIfAbsent(cipher) { CipherContext(algorithmType = algorithm) }
                    ctx.algorithmType = algorithm
                    appendAadInput(
                        ctx = ctx,
                        args = param.args,
                        byteBufferInput = updateAadByteBufferInput.get()
                    )
                } finally {
                    updateAadByteBufferInput.remove()
                }
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "update", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val input = param.args.getOrNull(0) as? ByteBuffer
                val output = param.args.getOrNull(1) as? ByteBuffer
                updateByteBufferInput.set(input?.let { RecordOutHelper.captureBytes(it) })
                updateByteBufferOutputPosition.set(output?.position())
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val cipher = param.thisObject as? Cipher ?: return
                    val algorithm = cipherContexts[cipher]?.algorithmType ?: cipher.algorithm ?: "unknown"
                    if (!AlgorithmRecordFilter.shouldRecordCipher(
                            algorithm,
                            extensionConfig.algorithmConfig.cipherOptions
                        )
                    ) {
                        cipherContexts.remove(cipher)
                        return
                    }
                    val ctx = cipherContexts.computeIfAbsent(cipher) { CipherContext(algorithmType = algorithm) }
                    ctx.algorithmType = algorithm
                    appendInput(
                        ctx = ctx,
                        args = param.args,
                        byteBufferInput = updateByteBufferInput.get()
                    )
                    appendOutput(
                        ctx = ctx,
                        args = param.args,
                        result = param.result,
                        byteBufferOutputPosition = updateByteBufferOutputPosition.get()
                    )
                } finally {
                    updateByteBufferInput.remove()
                    updateByteBufferOutputPosition.remove()
                }
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "doFinal", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val input = param.args.getOrNull(0) as? ByteBuffer
                val output = param.args.getOrNull(1) as? ByteBuffer
                doFinalByteBufferInput.set(input?.let { RecordOutHelper.captureBytes(it) })
                doFinalByteBufferOutputPosition.set(output?.position())
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val cipher = param.thisObject as? Cipher ?: return
                    val ctx = cipherContexts.remove(cipher) ?: CipherContext()
                    val algorithm = ctx.algorithmType ?: cipher.algorithm ?: "unknown"
                    if (!AlgorithmRecordFilter.shouldRecordCipher(
                            algorithm,
                            extensionConfig.algorithmConfig.cipherOptions
                        )
                    ) {
                        return
                    }

                    appendInput(
                        ctx = ctx,
                        args = param.args,
                        byteBufferInput = doFinalByteBufferInput.get()
                    )
                    appendOutput(
                        ctx = ctx,
                        args = param.args,
                        result = param.result,
                        byteBufferOutputPosition = doFinalByteBufferOutputPosition.get()
                    )

                    outputCipherRecord(algorithm, ctx, ctx.cryptType)
                } finally {
                    doFinalByteBufferInput.remove()
                    doFinalByteBufferOutputPosition.remove()
                }
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "wrap", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val ctx = cipherContexts.remove(cipher) ?: CipherContext(cryptType = RecordCipherMode.Wrap)
                val algorithm = ctx.algorithmType ?: cipher.algorithm ?: "unknown"
                if (!AlgorithmRecordFilter.shouldRecordCipher(
                        algorithm,
                        extensionConfig.algorithmConfig.cipherOptions
                    )
                ) {
                    return
                }

                val keyArg = param.args.getOrNull(0) as? Key
                val inputBytes = runCatching { keyArg?.encoded }.getOrNull()
                if (inputBytes != null) {
                    writeWithCap(ctx.dataStream, inputBytes, 0, inputBytes.size)
                }
                val resultBytes = param.result as? ByteArray
                if (resultBytes != null) {
                    writeWithCap(ctx.resultStream, resultBytes, 0, resultBytes.size)
                }
                outputCipherRecord(algorithm, ctx, RecordCipherMode.Wrap)
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "unwrap", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cipher = param.thisObject as? Cipher ?: return
                val ctx = cipherContexts.remove(cipher) ?: CipherContext(cryptType = RecordCipherMode.Unwrap)
                val algorithm = ctx.algorithmType ?: cipher.algorithm ?: "unknown"
                if (!AlgorithmRecordFilter.shouldRecordCipher(
                        algorithm,
                        extensionConfig.algorithmConfig.cipherOptions
                    )
                ) {
                    return
                }

                val wrappedKey = param.args.getOrNull(0) as? ByteArray
                if (wrappedKey != null) {
                    writeWithCap(ctx.dataStream, wrappedKey, 0, wrappedKey.size)
                }
                val resultKey = param.result as? Key
                val resultBytes = runCatching { resultKey?.encoded }.getOrNull()
                if (resultBytes != null) {
                    writeWithCap(ctx.resultStream, resultBytes, 0, resultBytes.size)
                }
                outputCipherRecord(algorithm, ctx, RecordCipherMode.Unwrap)
            }
        })
    }

    private fun appendAadInput(
        ctx: CipherContext,
        args: Array<Any?>,
        byteBufferInput: ByteArray?
    ) {
        when {
            args.size == 1 && args[0] is ByteArray -> {
                val input = args[0] as ByteArray
                writeWithCap(ctx.aadStream, input, 0, input.size)
            }

            args.size == 1 && args[0] is ByteBuffer -> {
                byteBufferInput?.let { writeWithCap(ctx.aadStream, it) }
            }

            args.size == 3 && args[0] is ByteArray -> {
                val input = args[0] as ByteArray
                val offset = args[1] as? Int ?: return
                val len = args[2] as? Int ?: return
                if (offset >= 0 && len >= 0 && offset + len <= input.size) {
                    writeWithCap(ctx.aadStream, input, offset, len)
                }
            }
        }
    }

    private fun appendInput(
        ctx: CipherContext,
        args: Array<Any?>,
        byteBufferInput: ByteArray?
    ) {
        when {
            args.isEmpty() -> Unit

            args.size == 1 -> {
                when (val p0 = args[0]) {
                    is ByteArray -> writeWithCap(ctx.dataStream, p0, 0, p0.size)
                    is Byte -> writeWithCap(ctx.dataStream, byteArrayOf(p0))
                    is ByteBuffer -> byteBufferInput?.let { writeWithCap(ctx.dataStream, it) }
                }
            }

            args.size == 2 && args[0] is ByteBuffer -> {
                byteBufferInput?.let { writeWithCap(ctx.dataStream, it) }
            }

            args.size in 3..5 && args[0] is ByteArray -> {
                val input = args[0] as ByteArray
                val offset = args[1] as? Int ?: return
                val len = args[2] as? Int ?: return
                if (offset >= 0 && len >= 0 && offset + len <= input.size) {
                    writeWithCap(ctx.dataStream, input, offset, len)
                }
            }
        }
    }

    private fun appendOutput(
        ctx: CipherContext,
        args: Array<Any?>,
        result: Any?,
        byteBufferOutputPosition: Int?
    ) {
        when (result) {
            is ByteArray -> writeWithCap(ctx.resultStream, result, 0, result.size)
            is Int -> {
                val outputBytes = when {
                    args.size == 2 && args[0] is ByteArray -> {
                        copyOutputBytes(args[0] as ByteArray, args[1] as? Int, result)
                    }

                    args.size == 4 && args[3] is ByteArray -> {
                        copyOutputBytes(args[3] as ByteArray, 0, result)
                    }

                    args.size == 5 && args[3] is ByteArray -> {
                        copyOutputBytes(args[3] as ByteArray, args[4] as? Int, result)
                    }

                    args.size == 2 && args[1] is ByteBuffer -> {
                        copyOutputBytes(args[1] as ByteBuffer, byteBufferOutputPosition, result)
                    }

                    else -> null
                }
                outputBytes?.let { writeWithCap(ctx.resultStream, it, 0, it.size) }
            }
        }
    }

    private fun copyOutputBytes(output: ByteArray, offset: Int?, written: Int): ByteArray? {
        val safeOffset = offset ?: return null
        if (written < 0 || safeOffset < 0 || safeOffset > output.size) return null
        if (written > output.size - safeOffset) return null
        return RecordOutHelper.captureBytes(output, safeOffset, written)
    }

    private fun copyOutputBytes(output: ByteBuffer, startPosition: Int?, written: Int): ByteArray? {
        val safeStart = startPosition ?: return null
        if (written < 0) return null
        val duplicate = output.duplicate()
        if (safeStart < 0 || safeStart > duplicate.limit()) return null
        if (written > duplicate.limit() - safeStart) return null
        duplicate.position(safeStart)
        duplicate.limit(safeStart + written)
        return RecordOutHelper.captureBytes(duplicate)
    }

    private fun outputCipherRecord(
        algorithm: String,
        ctx: CipherContext,
        cryptType: RecordCipherMode
    ) {
        RecordOutHelper.outputRecord(
            type = RecordType.Cipher, subType = algorithm, record = RecordCipher(
                algorithm = algorithm,
                cryptType = cryptType,
                key = ctx.keyBase,
                iv = ctx.iv,
                aad = ctx.aadStream.toByteArray().takeIf { it.isNotEmpty() }?.recordValue,
                rawData = ctx.dataStream.toByteArray().recordValue,
                resultData = ctx.resultStream.toByteArray().recordValue,
                stackDetail = RecordOutHelper.getStackTraceStr()
            )
        )
    }
}
