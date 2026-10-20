package com.thegreatyamori.saldos.data.cnt

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Port of the CNT web client's CryptoJS encryption.
 *
 * CryptoJS with a passphrase string uses the OpenSSL KDF: the output is
 * `base64("Salted__" + salt + ciphertext)`, AES-256-CBC, salt random 8 bytes.
 * The `iv` option of the original JS is ignored by the KDF.
 */
object CntCrypto {
    private const val PASSPHRASE = "a)b5g&mE9SHHtk}d"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val SALT_SIZE = 8
    private const val KEY_SIZE = 32
    private const val IV_SIZE = 16
    private val SALT_HEADER = "Salted__".toByteArray(Charsets.US_ASCII)

    fun encrypt(plain: String, salt: ByteArray = randomSalt()): String {
        require(salt.size == SALT_SIZE) { "salt must be $SALT_SIZE bytes" }
        val keyIv = evpBytesToKey(PASSPHRASE.toByteArray(Charsets.UTF_8), salt, KEY_SIZE + IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyIv.copyOfRange(0, KEY_SIZE), "AES"),
            IvParameterSpec(keyIv.copyOfRange(KEY_SIZE, KEY_SIZE + IV_SIZE))
        )
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(SALT_HEADER + salt + ciphertext)
    }

    fun decrypt(encoded: String): String {
        val raw = Base64.getDecoder().decode(encoded)
        require(raw.size > SALT_HEADER.size + SALT_SIZE) { "invalid ciphertext" }
        require(raw.copyOfRange(0, SALT_HEADER.size).contentEquals(SALT_HEADER)) {
            "missing Salted__ header"
        }
        val salt = raw.copyOfRange(SALT_HEADER.size, SALT_HEADER.size + SALT_SIZE)
        val ciphertext = raw.copyOfRange(SALT_HEADER.size + SALT_SIZE, raw.size)
        val keyIv = evpBytesToKey(PASSPHRASE.toByteArray(Charsets.UTF_8), salt, KEY_SIZE + IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyIv.copyOfRange(0, KEY_SIZE), "AES"),
            IvParameterSpec(keyIv.copyOfRange(KEY_SIZE, KEY_SIZE + IV_SIZE))
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun hexSalt(hex: String): ByteArray {
        require(hex.length == SALT_SIZE * 2) { "salt hex must be ${SALT_SIZE * 2} chars" }
        return ByteArray(SALT_SIZE) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun randomSalt(): ByteArray = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)

    private fun evpBytesToKey(password: ByteArray, salt: ByteArray, length: Int): ByteArray {
        val digest = MessageDigest.getInstance("MD5")
        val result = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        while (offset < length) {
            digest.reset()
            digest.update(previous)
            digest.update(password)
            digest.update(salt)
            previous = digest.digest()
            val take = minOf(previous.size, length - offset)
            previous.copyInto(result, offset, 0, take)
            offset += take
        }
        return result
    }
}
