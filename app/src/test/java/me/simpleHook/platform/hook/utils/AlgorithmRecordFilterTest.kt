package me.simpleHook.platform.hook.utils

import me.simpleHook.data.ExCipherOptions
import me.simpleHook.data.ExHmacOptions
import me.simpleHook.data.ExMessageDigestOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmRecordFilterTest {

    @Test
    fun shouldRecordDigest_routesCommonAlgorithmsToDedicatedOptions() {
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("MD5", digestOnly { md5 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA", digestOnly { sha1 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA-1", digestOnly { sha1 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA-224", digestOnly { sha224 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA-256", digestOnly { sha256 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA-384", digestOnly { sha384 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA-512/256", digestOnly { sha512 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHA3-256", digestOnly { sha3 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SHAKE256", digestOnly { sha3 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("SM3", digestOnly { sm3 = true }))
    }

    @Test
    fun shouldRecordDigest_usesOtherOnlyForUnclassifiedAlgorithms() {
        assertFalse(AlgorithmRecordFilter.shouldRecordDigest("MD5", digestOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("MD2", digestOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("MD4", digestOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("RIPEMD160", digestOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("BLAKE2B-512", digestOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordDigest("Whirlpool", digestOnly { other = true }))
        assertFalse(AlgorithmRecordFilter.shouldRecordDigest("Whirlpool", digestOnly()))
    }

    @Test
    fun shouldRecordHmac_routesCommonAlgorithmsToDedicatedOptions() {
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacMD5", hmacOnly { hmacMd5 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HMAC/SHA1", hmacOnly { hmacSha1 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA224", hmacOnly { hmacSha224 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA256", hmacOnly { hmacSha256 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA384", hmacOnly { hmacSha384 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA512/256", hmacOnly { hmacSha512 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA3-256", hmacOnly { hmacSha3 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacSM3", hmacOnly { hmacSm3 = true }))
    }

    @Test
    fun shouldRecordHmac_usesOtherOnlyForUnclassifiedAlgorithms() {
        assertFalse(AlgorithmRecordFilter.shouldRecordHmac("HmacSHA256", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacRIPEMD160", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("HmacBLAKE2b", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("AESCMAC", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("AES-GMAC", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("Poly1305-AES", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("SipHash-2-4", hmacOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordHmac("KMAC128", hmacOnly { other = true }))
        assertFalse(AlgorithmRecordFilter.shouldRecordHmac("KMAC128", hmacOnly()))
    }

    @Test
    fun shouldRecordCipher_routesCommonAlgorithmsToDedicatedOptions() {
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("AES/GCM/NoPadding", cipherOnly { aes = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("DES/CBC/PKCS5Padding", cipherOnly { des = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("DESede/CBC/PKCS5Padding", cipherOnly { tripleDes = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("3DES", cipherOnly { tripleDes = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("RSA/ECB/PKCS1Padding", cipherOnly { rsa = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("SM2", cipherOnly { sm2 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("SM4/CBC/PKCS5Padding", cipherOnly { sm4 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("ChaCha20-Poly1305", cipherOnly { chacha20 = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("ARC4", cipherOnly { rc4 = true }))
    }

    @Test
    fun shouldRecordCipher_usesOtherOnlyForUnclassifiedAlgorithms() {
        assertFalse(AlgorithmRecordFilter.shouldRecordCipher("AES/GCM/NoPadding", cipherOnly { other = true }))
        assertFalse(
            AlgorithmRecordFilter.shouldRecordCipher(
                "PBEWithHmacSHA256AndAES_256",
                cipherOnly { aes = true }
            )
        )
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("PBEWithHmacSHA256AndAES_256", cipherOnly { pbe = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("ECIESwithAES-CBC", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Blowfish/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Twofish/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("IDEA/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Camellia/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("SEED/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("CAST5/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("RC2/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("RC5/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("ARIA/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Serpent/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Salsa20", cipherOnly { other = true }))
        assertTrue(AlgorithmRecordFilter.shouldRecordCipher("Noekeon/CBC/PKCS5Padding", cipherOnly { other = true }))
        assertFalse(AlgorithmRecordFilter.shouldRecordCipher("Noekeon/CBC/PKCS5Padding", cipherOnly()))
    }

    private fun digestOnly(configure: ExMessageDigestOptions.() -> Unit = {}): ExMessageDigestOptions {
        return ExMessageDigestOptions(
            md5 = false,
            sha1 = false,
            sha224 = false,
            sha256 = false,
            sha384 = false,
            sha512 = false,
            sha3 = false,
            sm3 = false,
            other = false
        ).apply(configure)
    }

    private fun hmacOnly(configure: ExHmacOptions.() -> Unit = {}): ExHmacOptions {
        return ExHmacOptions(
            hmacMd5 = false,
            hmacSha1 = false,
            hmacSha224 = false,
            hmacSha256 = false,
            hmacSha384 = false,
            hmacSha512 = false,
            hmacSha3 = false,
            hmacSm3 = false,
            other = false
        ).apply(configure)
    }

    private fun cipherOnly(configure: ExCipherOptions.() -> Unit = {}): ExCipherOptions {
        return ExCipherOptions(
            aes = false,
            des = false,
            tripleDes = false,
            rsa = false,
            sm2 = false,
            sm4 = false,
            chacha20 = false,
            rc4 = false,
            pbe = false,
            other = false
        ).apply(configure)
    }
}
