package me.simpleHook.platform.hook.utils

import me.simpleHook.data.ExCipherOptions
import me.simpleHook.data.ExHmacOptions
import me.simpleHook.data.ExMessageDigestOptions
import java.util.Locale

object AlgorithmRecordFilter {

    fun shouldRecordDigest(algorithm: String?, options: ExMessageDigestOptions): Boolean {
        val key = algorithm.normalizedAlgorithmKey()
        return when {
            key == "SHA" -> options.sha1
            key == "SHA1" -> options.sha1
            key == "SHA224" -> options.sha224
            key == "SHA256" -> options.sha256
            key == "SHA384" -> options.sha384
            key == "SHA512" || key.startsWith("SHA512") -> options.sha512
            key.startsWith("SHA3") || key.startsWith("SHAKE") -> options.sha3
            else -> when (key) {
                "MD5" -> options.md5
                "SM3" -> options.sm3
                else -> options.other
            }
        }
    }

    fun shouldRecordHmac(algorithm: String?, options: ExHmacOptions): Boolean {
        val digestKey = algorithm.normalizedHmacDigestKey()
        return when (digestKey) {
            "MD5" -> options.hmacMd5
            "SHA1" -> options.hmacSha1
            "SHA224" -> options.hmacSha224
            "SHA256" -> options.hmacSha256
            "SHA384" -> options.hmacSha384
            "SHA512" -> options.hmacSha512
            "SM3" -> options.hmacSm3
            else -> when {
                digestKey.startsWith("SHA512") -> options.hmacSha512
                digestKey.matchesAnyPrefix("SHA3", "SHAKE") -> options.hmacSha3
                else -> options.other
            }
        }
    }

    fun shouldRecordCipher(transformation: String?, options: ExCipherOptions): Boolean {
        val key = transformation
            ?.substringBefore("/")
            .normalizedAlgorithmKey()

        return when {
            key.startsWith("PBE") -> options.pbe
            key.isTripleDes() -> options.tripleDes
            key == "DES" -> options.des
            key == "AES" || key.startsWith("AES") -> options.aes
            key == "RSA" || key.startsWith("RSA") -> options.rsa
            key == "SM2" || key.startsWith("SM2") -> options.sm2
            key == "SM4" || key.startsWith("SM4") -> options.sm4
            key == "CHACHA20" || key.startsWith("CHACHA") || key.startsWith("XCHACHA") -> options.chacha20
            key == "RC4" || key == "ARC4" || key == "ARCFOUR" || key.startsWith("RC4") -> options.rc4
            else -> options.other
        }
    }

    private fun String?.normalizedAlgorithmKey(): String {
        return orEmpty()
            .uppercase(Locale.US)
            .filter { it in 'A'..'Z' || it in '0'..'9' }
    }

    private fun String?.normalizedHmacDigestKey(): String {
        val key = normalizedAlgorithmKey()
        return when {
            key.startsWith("HMACWITH") -> key.removePrefix("HMACWITH")
            key.startsWith("HMAC") -> key.removePrefix("HMAC")
            key.startsWith("WITH") -> key.removePrefix("WITH")
            key.contains("HMAC") -> key.substringAfter("HMAC")
            else -> key
        }
    }

    private fun String.matchesAnyPrefix(vararg prefixes: String): Boolean {
        return prefixes.any { startsWith(it.normalizedAlgorithmKey()) }
    }

    private fun String.isTripleDes(): Boolean {
        return startsWith("3DES") ||
                startsWith("TDEA") ||
                startsWith("DESEDE") ||
                startsWith("TRIPLEDES")
    }
}
