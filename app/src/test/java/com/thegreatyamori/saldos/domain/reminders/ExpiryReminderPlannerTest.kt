package com.thegreatyamori.saldos.domain.reminders

import com.thegreatyamori.saldos.domain.CloseReason
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.TrackedPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExpiryReminderPlannerTest {

    private val planner = ExpiryReminderPlanner()
    private val zone = ZoneId.of("UTC")

    @Test
    fun `does not remind more than three days before expiry`() {
        val now = Instant.parse("2026-11-12T10:00:00Z")

        val result = planner.plan(
            packages = listOf(pkg("2026-11-16T10:00:00Z")),
            now = now,
            zone = zone,
            history = PlannerFakeHistory()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `reminds at three days and reduces the count each day`() {
        val history = PlannerFakeHistory()
        val tracked = pkg("2026-11-15T10:00:00Z")

        val threeDays = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )
        val twoDays = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-13T10:00:00Z"),
            zone,
            history
        )

        assertEquals(ReminderKind.DAILY, threeDays.single().kind)
        assertEquals(3, threeDays.single().daysLeft ?: -1)
        assertEquals(2, twoDays.single().daysLeft ?: -1)
    }

    @Test
    fun `uses today and tomorrow wording boundaries`() {
        val history = PlannerFakeHistory()

        val today = planner.plan(
            listOf(pkg("2026-11-12T20:00:00Z")),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )
        val tomorrow = planner.plan(
            listOf(pkg("2026-11-13T20:00:00Z")),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )

        assertEquals(0, today.single().daysLeft ?: -1)
        assertEquals(1, tomorrow.single().daysLeft ?: -1)
    }

    @Test
    fun `respects the quiet hours window`() {
        val tracked = pkg("2026-11-14T10:00:00Z")
        val history = PlannerFakeHistory()

        val before = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T07:00:00Z"),
            zone,
            history
        )
        val atStart = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T08:00:00Z"),
            zone,
            history
        )
        val atEnd = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T22:00:00Z"),
            zone,
            history
        )

        assertTrue(before.isEmpty())
        assertEquals(ReminderKind.DAILY, atStart.single().kind)
        assertTrue(atEnd.isEmpty())
    }

    @Test
    fun `does not repeat the daily reminder on the same day`() {
        val tracked = pkg("2026-11-15T10:00:00Z")
        val history = PlannerFakeHistory().apply {
            recordDaily(LocalDate.parse("2026-11-12"))
        }

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `does not repeat the daily reminder across packages on the same day`() {
        val first = pkg("2026-11-15T10:00:00Z", id = 1)
        val second = pkg("2026-11-16T10:00:00Z", id = 2)
        val history = PlannerFakeHistory().apply {
            recordDaily(LocalDate.parse("2026-11-12"))
        }

        val result = planner.plan(
            listOf(first, second),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `reports an expired package once`() {
        val tracked = pkg("2026-11-12T09:00:00Z")

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertEquals(ReminderKind.EXPIRED, result.single().kind)
    }

    @Test
    fun `reports a consumed package before its expiry`() {
        val tracked = pkg(
            "2026-11-20T10:00:00Z",
            closed = true,
            closeReason = CloseReason.CONSUMED
        )

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertEquals(ReminderKind.CONSUMED, result.single().kind)
    }

    @Test
    fun `does not report an unconfirmed missing package as consumed`() {
        val tracked = pkg(
            "2026-11-20T10:00:00Z",
            closed = true,
            closeReason = CloseReason.MISSING
        )

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `reports a consumed package without a valid expiry`() {
        val tracked = pkg(
            "not-a-date",
            closed = true,
            closeReason = CloseReason.CONSUMED
        )

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertEquals(ReminderKind.CONSUMED, result.single().kind)
        assertNull(result.single().expiresAt)
    }

    @Test
    fun `does not repeat a final reminder`() {
        val tracked = pkg("2026-11-12T09:00:00Z")
        val history = PlannerFakeHistory().apply { recordFinal(tracked.reminderKey()) }

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            history
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `marks inferred packages as estimated`() {
        val tracked = pkg("2026-11-15T10:00:00Z", source = PackageSource.DIFF)

        val result = planner.plan(
            listOf(tracked),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertTrue(result.single().estimated)
    }

    @Test
    fun `only reminds the soonest open package daily`() {
        val soon = pkg("2026-11-14T10:00:00Z", id = 1)
        val later = pkg("2026-11-15T10:00:00Z", id = 2)

        val result = planner.plan(
            listOf(later, soon),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertEquals(soon.reminderKey(), result.single().key)
    }

    @Test
    fun `reports a final reminder per package`() {
        val first = pkg("2026-11-12T09:00:00Z", id = 1)
        val second = pkg("2026-11-10T09:00:00Z", id = 2)

        val result = planner.plan(
            listOf(first, second),
            Instant.parse("2026-11-12T10:00:00Z"),
            zone,
            PlannerFakeHistory()
        )

        assertEquals(2, result.count { it.kind == ReminderKind.EXPIRED })
    }

    private fun pkg(
        expiresAt: String,
        source: PackageSource = PackageSource.SMS,
        closed: Boolean = false,
        closeReason: CloseReason? = null,
        id: Long = expiresAt.hashCode().toLong()
    ) = TrackedPackage(
        smsId = id,
        dataMb = 1024,
        days = 30,
        startedAt = "2026-11-01T10:00:00Z",
        expiresAt = expiresAt,
        source = source,
        closed = closed,
        closeReason = closeReason
    )

    private class PlannerFakeHistory : ReminderHistory {
        private var daily: LocalDate? = null
        private val finals = mutableSetOf<String>()
        private var baseline = false

        override fun lastDailyDate(): LocalDate? = daily

        override fun finalSent(key: String): Boolean = key in finals

        override fun recordDaily(date: LocalDate) {
            daily = date
        }

        override fun recordFinal(key: String) {
            finals += key
        }

        override fun baselineDone(): Boolean = baseline

        override fun markBaselineDone() {
            baseline = true
        }

        override fun prune(activeKeys: Set<String>) = Unit
    }
}
