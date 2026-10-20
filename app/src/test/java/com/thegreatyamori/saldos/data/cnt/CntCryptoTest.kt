package com.thegreatyamori.saldos.data.cnt

import org.junit.Assert.assertEquals
import org.junit.Test

class CntCryptoTest {

    @Test
    fun `matches the CryptoJS OpenSSL output with a fixed salt`() {
        val salt = CntCrypto.hexSalt("0001020304050607")
        val encrypted = CntCrypto.encrypt("0983592938", salt)
        assertEquals("U2FsdGVkX18AAQIDBAUGB2nK5ywcW9r5tFrycTI3V+Y=", encrypted)
    }

    @Test
    fun `decrypts the CryptoJS fixture`() {
        val encrypted = "U2FsdGVkX18AAQIDBAUGB2nK5ywcW9r5tFrycTI3V+Y="
        assertEquals("0983592938", CntCrypto.decrypt(encrypted))
    }

    @Test
    fun `round trips with a random salt`() {
        val encrypted = CntCrypto.encrypt("a)b5g&mE9SHHtk}d")
        assertEquals("a)b5g&mE9SHHtk}d", CntCrypto.decrypt(encrypted))
    }
}
