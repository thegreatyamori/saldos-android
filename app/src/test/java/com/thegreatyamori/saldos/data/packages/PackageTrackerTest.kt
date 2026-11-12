package com.thegreatyamori.saldos.data.packages

import com.thegreatyamori.saldos.data.sms.DaysSource
import com.thegreatyamori.saldos.data.sms.SmsActivation
import com.thegreatyamori.saldos.domain.BalanceItem
import com.thegreatyamori.saldos.domain.CloseReason
import com.thegreatyamori.saldos.domain.ExpiryMode
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.domain.SnapshotHeader
import com.thegreatyamori.saldos.domain.SnapshotLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PackageTrackerTest {

    private val tracker = PackageTracker()
    private val start = Instant.parse("2026-10-27T14:32:10Z")

    @Test
    fun `creates a package from the activation SMS and matches the quota`() {
        val state = tracker.reconcile(
            state = ActivationState(),
            previous = null,
            current = snapshot(total = "5.00", remaining = "5"),
            activations = listOf(sms(1)),
            now = start.plusSeconds(60)
        )

        assertEquals(1, state.packages.size)
        val pkg = state.packages.single()
        assertEquals(PackageSource.SMS, pkg.source)
        assertEquals("GIGAS", pkg.label)
        assertEquals(30, pkg.days)
        assertEquals(start.plusSeconds(30L * 24 * 60 * 60).toString(), pkg.expiresAt)
        assertFalse(pkg.closed)
    }

    @Test
    fun `does not duplicate the same SMS on later refreshes`() {
        val first = tracker.reconcile(ActivationState(), null, snapshot(), listOf(sms(1)), start)
        val second = tracker.reconcile(first, snapshot(), snapshot(), listOf(sms(1)), start)

        assertEquals(1, second.packages.size)
    }

    @Test
    fun `infers an activation from a quota jump when there is no SMS`() {
        val previous = snapshot(total = "1.00", remaining = "0.4", fetchedAt = "2026-10-20T10:00:00Z")
        val state = tracker.reconcile(
            state = ActivationState(),
            previous = previous,
            current = snapshot(total = "5.00", remaining = "5"),
            activations = emptyList(),
            now = Instant.parse("2026-10-20T10:15:00Z")
        )

        val pkg = state.packages.single()
        assertEquals(PackageSource.DIFF, pkg.source)
        assertEquals(30, pkg.days)
        assertEquals("2026-10-20T10:00:00Z", pkg.startedAt)
    }

    @Test
    fun `does not infer anything when the quota did not change`() {
        val previous = snapshot(total = "5.00", remaining = "4.2", fetchedAt = "2026-10-20T10:00:00Z")
        val state = tracker.reconcile(
            state = ActivationState(),
            previous = previous,
            current = snapshot(total = "5.00", remaining = "4.1"),
            activations = emptyList(),
            now = Instant.parse("2026-10-20T10:15:00Z")
        )

        assertTrue(state.packages.isEmpty())
    }

    @Test
    fun `keeps the SMS package open until CNT reflects the new quota`() {
        val state = tracker.reconcile(
            state = ActivationState(),
            previous = null,
            current = snapshot(total = "1.00", remaining = "0.2"),
            activations = listOf(sms(1)),
            now = start.plusSeconds(60)
        )

        val pkg = state.packages.single()
        assertFalse(pkg.closed)
        assertFalse(pkg.matched)
    }

    @Test
    fun `closes the package when the quota disappears`() {
        val active = tracker.reconcile(ActivationState(), null, snapshot(), listOf(sms(1)), start)
        val closed = tracker.reconcile(
            state = active,
            previous = snapshot(),
            current = snapshot(total = "0.00", remaining = "0"),
            activations = emptyList(),
            now = start.plusSeconds(60)
        )

        assertTrue(closed.packages.single().closed)
        assertEquals(CloseReason.CONSUMED, closed.packages.single().closeReason)
    }

    @Test
    fun `marks a package as missing when the quota item disappears`() {
        val active = tracker.reconcile(ActivationState(), null, snapshot(), listOf(sms(1)), start)
        val closed = tracker.reconcile(
            state = active,
            previous = snapshot(),
            current = snapshot().copy(hero = null),
            activations = emptyList(),
            now = start.plusSeconds(60)
        )

        assertEquals(CloseReason.MISSING, closed.packages.single().closeReason)
    }

    @Test
    fun `learns the day-end expiry model when the quota dies at midnight`() {
        val daily = SmsActivation(
            smsId = 7,
            startedAt = start,
            priceUsd = 1,
            dataMb = 1024,
            days = 1,
            daysSource = DaysSource.SMS,
            unlimited = false
        )
        val active = tracker.reconcile(
            ActivationState(),
            null,
            snapshot(total = "1.00", remaining = "1"),
            listOf(daily),
            start
        )
        val lateAtNight = LocalDate.of(2026, 10, 28)
            .atTime(23, 59, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val closed = tracker.reconcile(
            state = active,
            previous = snapshot(total = "1.00", remaining = "0.02"),
            current = snapshot(total = "0.00", remaining = "0"),
            activations = emptyList(),
            now = lateAtNight
        )

        assertEquals(ExpiryMode.DAY_END, closed.packages.single().expiryMode)
    }

    private fun sms(id: Long) = SmsActivation(
        smsId = id,
        startedAt = start,
        priceUsd = 5,
        dataMb = 5120,
        days = 30,
        daysSource = DaysSource.SMS,
        unlimited = false
    )

    private fun snapshot(
        total: String = "5.00",
        remaining: String = "5",
        fetchedAt: String = "2026-10-27T14:33:00Z"
    ) = Snapshot(
        serviceType = "movil",
        header = SnapshotHeader(phone = "0983592938", planTitle = "PLAN PREPAGO CHIP PLUS"),
        line = SnapshotLine(phone = "0983592938", phoneValue = "servicio-0"),
        hero = BalanceItem(
            label = "GIGAS",
            value = remaining,
            unit = "GB",
            total = total,
            ratio = 0.5f,
            ring = true,
            color = "#01A0E4"
        ),
        items = emptyList(),
        lines = emptyList(),
        fetchedAt = fetchedAt
    )
}
