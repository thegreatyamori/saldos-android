package com.thegreatyamori.saldos.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CntSmsParserTest {

    private val parser = CntSmsParser()

    @Test
    fun `parses the real CNT activation SMS`() {
        val activation = parser.parse(
            sms("CNT informa has activado tu paquete de \$5 5GB 50Min, 30 Min inter, Min ilimitados CNT, FW, WA, IG,TIK, vigencia 30 dias")
        )

        assertEquals(5, activation?.priceUsd)
        assertEquals(5120, activation?.dataMb)
        assertEquals(30, activation?.days)
        assertEquals(DaysSource.SMS, activation?.daysSource)
        assertEquals(Instant.parse("2026-10-27T14:32:10Z"), activation?.startedAt)
        assertEquals(false, activation?.catalogMismatch)
    }

    @Test
    fun `handles the singular one day combo`() {
        val activation = parser.parse(
            sms("CNT informa has activado tu paquete de \$1 1GB 15 Min, vigencia 1 dia")
        )

        assertEquals(1, activation?.days)
        assertEquals(1024, activation?.dataMb)
    }

    @Test
    fun `falls back to the catalog when the SMS omits the duration`() {
        val activation = parser.parse(
            sms("CNT informa has activado tu paquete de \$6 7GB 100 Min")
        )

        assertEquals(30, activation?.days)
        assertEquals(DaysSource.CATALOG_PRICE, activation?.daysSource)
    }

    @Test
    fun `keeps the SMS duration and flags a catalog mismatch`() {
        val activation = parser.parse(
            sms("CNT informa has activado tu paquete de \$5 5GB 50Min, vigencia 7 dias")
        )

        assertEquals(7, activation?.days)
        assertEquals(true, activation?.catalogMismatch)
    }

    @Test
    fun `detects the unlimited combo`() {
        val activation = parser.parse(
            sms("CNT informa has activado tu paquete de \$25 Min ilimitados, vigencia 30 dias")
        )

        assertEquals(true, activation?.unlimited)
        assertEquals(0, activation?.dataMb)
        assertEquals(30, activation?.days)
    }

    @Test
    fun `ignores unrelated messages`() {
        assertNull(parser.parse(sms("Su recarga de \$10 fue exitosa")))
        assertNull(parser.parse(sms("CNT informa que su saldo es \$1.20")))
    }

    @Test
    fun `accepts accents and uppercase`() {
        val activation = parser.parse(
            sms("CNT INFORMA HAS ACTIVADO TU PAQUETE DE \$3 4GB VIGENCIA 7 DÍAS")
        )

        assertTrue(activation != null)
        assertEquals(7, activation?.days)
    }

    private fun sms(body: String, id: Long = 1L, date: String = "2026-10-27T14:32:10Z") =
        RawSms(id = id, address = "777", body = body, dateMs = Instant.parse(date).toEpochMilli())
}
