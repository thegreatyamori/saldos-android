package com.thegreatyamori.saldos.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CntCatalogTest {

    @Test
    fun `prices are unique`() {
        val prices = CntCatalog.all.map { it.priceUsd }
        assertEquals(prices.size, prices.toSet().size)
    }

    @Test
    fun `data sizes are unique except unlimited`() {
        val sizes = CntCatalog.all.map { it.dataMb }.filter { it > 0 }
        assertEquals(sizes.size, sizes.toSet().size)
    }

    @Test
    fun `catalog is ordered and every duration is positive`() {
        val prices = CntCatalog.all.map { it.priceUsd }
        assertEquals(prices.sorted(), prices)
        CntCatalog.all.forEach { pkg ->
            assert(pkg.days > 0) { "package ${pkg.priceUsd} has no duration" }
        }
    }

    @Test
    fun `maps the combo used by the real SMS`() {
        val five = CntCatalog.forPrice(5)
        assertNotNull(five)
        assertEquals(5120, five?.dataMb)
        assertEquals(30, five?.days)
    }

    @Test
    fun `maps daily, weekly and unlimited combos`() {
        assertEquals(1, CntCatalog.forPrice(1)?.days)
        assertEquals(512, CntCatalog.forPrice(1)?.socialMb)
        assertEquals(7, CntCatalog.forDataMb(4096)?.days)
        assertEquals(0, CntCatalog.forPrice(25)?.dataMb)
        assertEquals(30, CntCatalog.forPrice(25)?.days)
    }

    @Test
    fun `unknown price or size returns null`() {
        assertEquals(null, CntCatalog.forPrice(4))
        assertEquals(null, CntCatalog.forDataMb(9999))
    }

    @Test
    fun `approximate match tolerates rounding`() {
        assertEquals(30, CntCatalog.forDataMbApprox(5100)?.days)
        assertEquals(null, CntCatalog.forDataMbApprox(8000))
    }
}
