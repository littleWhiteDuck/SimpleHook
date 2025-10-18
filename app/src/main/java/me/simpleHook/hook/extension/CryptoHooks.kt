package me.simpleHook.hook.extension

import android.os.Build
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.data.record.RecordCipher
import me.simpleHook.data.record.RecordCipherMode
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.hook.utils.RecordOutHelper
import me.simpleHook.hook.utils.RecordOutHelper.recordValue
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.Key
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


// ------------ Hooks (AES/Cipher, Mac, MessageDigest) ------------
object CryptoHooks {
    // contexts per-instance
    private data class CipherContext(
        var keyBase: Map<RecordValueType, String>? = null,
        var keyAlgorithm: String? = null,
        var iv: Map<RecordValueType, String>? = null,
        var cryptType: RecordCipherMode = RecordCipherMode.Unknown,
        var algorithmType: String? = null,
        val dataStream: ByteArrayOutputStream = ByteArrayOutputStream(256)
    )

    private data class MacContext(
        var keyBase: Map<RecordValueType, String>? = null,
        var keyAlgorithm: String? = null,
        var algorithmType: String? = null,
        val dataStream: ByteArrayOutputStream = ByteArrayOutputStream(256)
    )

    private val cipherContexts = ConcurrentHashMap<Cipher, CipherContext>()
    private val macContexts = ConcurrentHashMap<Mac, MacContext>()
    private val digestBuffers = ConcurrentHashMap<MessageDigest, ByteArrayOutputStream>()

    // ---------- AES / Cipher Hook ----------
    fun hookCipher() {
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

                RecordOutHelper.outputRecord(
                    type = RecordType.Cipher, record = RecordCipher(
                        algorithm = ctx.algorithmType ?: "unknown",
                        cryptType = ctx.cryptType,
                        key = ctx.keyBase,
                        iv = ctx.iv,
                        rawData = ctx.dataStream.toByteArray().recordValue,
                        resultData = resultBytes?.recordValue ?: emptyMap(),
                        stackDetail = Throwable().stackTraceToString()
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

    // ---------- Mac (HMAC) Hook ----------
    fun hookMac() {
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

        XposedBridge.hookAllMethods(Mac::class.java, "update", object : XC_MethodHook() {
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

        XposedBridge.hookAllMethods(Mac::class.java, "doFinal", object : XC_MethodHook() {
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

                RecordOutHelper.outputMac(
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


    fun hookDigest() {
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


    private fun writeWithCap(
        stream: ByteArrayOutputStream,
        data: ByteArray,
        off: Int = 0,
        len: Int = data.size
    ) {
        // 1MB
        val remain = 1024 * 1024 - stream.size()
        if (remain <= 0) return
        val toWrite = kotlin.math.min(len, remain)
        try {
            stream.write(data, off, toWrite)
        } catch (_: Throwable) {

        }
    }

    // ---------- entrypoint to start all hooks  ----------
    fun startAllHooks(
        enableCipher: Boolean = true,
        enableMac: Boolean = true,
        enableDigest: Boolean = true
    ) {
        if (enableCipher) hookCipher()
        if (enableMac) hookMac()
        if (enableDigest) hookDigest()
    }
}
