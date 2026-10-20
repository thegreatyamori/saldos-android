package com.thegreatyamori.saldos.data.creds

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreCredentialStore(context: Context) : CredentialStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): Credentials? {
        val identifier = prefs.getString(KEY_IDENTIFIER, null) ?: return null
        val payload = prefs.getString(KEY_PASSWORD, null) ?: return null
        val password = decrypt(payload) ?: return null
        return Credentials(identifier = identifier, password = password)
    }

    override fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_IDENTIFIER, credentials.identifier)
            .putString(KEY_PASSWORD, encrypt(credentials.password))
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return encode(cipher.iv) + ":" + encode(ciphertext)
    }

    private fun decrypt(payload: String): String? = try {
        val parts = payload.split(":", limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, decode(parts[0]))
        )
        String(cipher.doFinal(decode(parts[1])), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val PREFS = "saldos.credentials"
        const val KEY_IDENTIFIER = "identifier"
        const val KEY_PASSWORD = "password"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "saldos.credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
