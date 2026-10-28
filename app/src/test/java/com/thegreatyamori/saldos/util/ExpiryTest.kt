package com.thegreatyamori.saldos.util

import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.TrackedPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiryTest {

    @Test
    fun `picks the soonest open package`() {
        val late = pkg(expiresAt = "2026-11-26T14:00:00Z")
        val soon = pkg(expiresAt = "2026-10-29T14:00:00Z")

        assertEquals(soon, activePackage(listOf(late, soon)))
    }

    @Test
    fun `ignores closed packages`() {
        val closed = pkg(expiresAt = "2026-10-29T14:00:00Z", closed = true)

        assertNull(activePackage(listOf(closed)))
    }

    private fun pkg(expiresAt: String, closed: Boolean = false) = TrackedPackage(
        smsId = expiresAt.hashCode().toLong(),
        dataMb = 5120,
        days = 30,
        startedAt = "2026-10-27T14:00:00Z",
        expiresAt = expiresAt,
        source = PackageSource.SMS,
        closed = closed
    )
}
