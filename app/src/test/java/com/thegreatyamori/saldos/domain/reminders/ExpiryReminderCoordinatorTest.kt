package com.thegreatyamori.saldos.domain.reminders

import com.thegreatyamori.saldos.domain.CloseReason
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.TrackedPackage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ExpiryReminderCoordinatorTest {

    private val zone = ZoneId.of("UTC")
    private val history = CoordinatorFakeHistory()
    private val sink = CoordinatorFakeSink()
    private var now = Instant.parse("2026-11-12T10:00:00Z")
    private var enabled = true

    @Test
    fun `sends one daily reminder per day`() = runTest {
        val tracked = pkg("2026-11-15T10:00:00Z")
        val coordinator = coordinator { listOf(tracked) }
        history.markBaselineDone()

        coordinator.check()
        coordinator.check()
        assertEquals(1, sink.sent.size)
        assertEquals(ReminderKind.DAILY, sink.sent.single().kind)

        now = now.plus(1, ChronoUnit.DAYS)
        coordinator.check()
        assertEquals(2, sink.sent.size)
    }

    @Test
    fun `baselines already due packages without notifying`() = runTest {
        val tracked = pkg("2026-11-12T09:00:00Z")
        val coordinator = coordinator { listOf(tracked) }

        coordinator.check()

        assertTrue(sink.sent.isEmpty())
        assertTrue(history.finalSent(tracked.reminderKey()))
        assertTrue(history.baselineDone())
    }

    @Test
    fun `baselines legacy closed packages without notifying`() = runTest {
        val tracked = pkg("2026-11-20T10:00:00Z", closed = true)
        val coordinator = coordinator { listOf(tracked) }

        coordinator.check()

        assertTrue(sink.sent.isEmpty())
        assertTrue(history.finalSent(tracked.reminderKey()))
    }

    @Test
    fun `sends a final reminder once after the baseline`() = runTest {
        val tracked = pkg("2026-11-12T09:00:00Z")
        val coordinator = coordinator { listOf(tracked) }
        history.markBaselineDone()

        coordinator.check()
        coordinator.check()

        assertEquals(1, sink.sent.size)
        assertEquals(ReminderKind.EXPIRED, sink.sent.single().kind)
    }

    @Test
    fun `reports consumed packages`() = runTest {
        val tracked = pkg(
            "2026-11-20T10:00:00Z",
            closed = true,
            closeReason = CloseReason.CONSUMED
        )
        val coordinator = coordinator { listOf(tracked) }
        history.markBaselineDone()

        coordinator.check()

        assertEquals(ReminderKind.CONSUMED, sink.sent.single().kind)
    }

    @Test
    fun `cancels notifications when disabled`() = runTest {
        enabled = false
        val coordinator = coordinator { emptyList() }

        coordinator.check()

        assertTrue(sink.cancelled)
        assertTrue(sink.sent.isEmpty())
    }

    @Test
    fun `retries when the sink rejects the notification`() = runTest {
        val tracked = pkg("2026-11-15T10:00:00Z")
        val coordinator = coordinator { listOf(tracked) }
        history.markBaselineDone()
        sink.accept = false

        coordinator.check()
        assertTrue(history.lastDailyDate() == null)

        sink.accept = true
        coordinator.check()
        assertEquals(1, sink.sent.size)
    }

    private fun coordinator(
        packages: () -> List<TrackedPackage>
    ) = ExpiryReminderCoordinator(
        packages = packages,
        enabled = { enabled },
        history = history,
        sink = sink,
        clock = { now },
        zone = { zone }
    )

    private fun pkg(
        expiresAt: String,
        closed: Boolean = false,
        closeReason: CloseReason? = null
    ) = TrackedPackage(
        smsId = expiresAt.hashCode().toLong(),
        dataMb = 1024,
        days = 30,
        startedAt = "2026-11-01T10:00:00Z",
        expiresAt = expiresAt,
        source = PackageSource.SMS,
        closed = closed,
        closeReason = closeReason
    )

    private class CoordinatorFakeHistory : ReminderHistory {
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

    private class CoordinatorFakeSink : ReminderSink {
        val sent = mutableListOf<ExpiryReminder>()
        var cancelled = false
        var accept = true

        override suspend fun notify(reminder: ExpiryReminder): Boolean {
            if (accept) sent += reminder
            return accept
        }

        override suspend fun cancelAll() {
            cancelled = true
        }
    }
}
