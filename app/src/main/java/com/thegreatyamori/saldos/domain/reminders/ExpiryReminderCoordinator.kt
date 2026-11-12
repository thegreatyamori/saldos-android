package com.thegreatyamori.saldos.domain.reminders

import com.thegreatyamori.saldos.domain.TrackedPackage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

class ExpiryReminderCoordinator(
    private val packages: () -> List<TrackedPackage>,
    private val enabled: () -> Boolean,
    private val history: ReminderHistory,
    private val sink: ReminderSink,
    private val planner: ExpiryReminderPlanner = ExpiryReminderPlanner(),
    private val clock: () -> Instant = { Instant.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) {

    private val mutex = Mutex()

    suspend fun check() = mutex.withLock {
        if (!enabled()) {
            sink.cancelAll()
            return@withLock
        }

        val now = clock()
        val currentPackages = packages()
        currentPackages
            .filter { it.closed && it.closeReason == null }
            .forEach { history.recordFinal(it.reminderKey()) }
        if (!history.baselineDone() && currentPackages.isNotEmpty()) {
            currentPackages
                .filter { it.hasFinalReminder(now) }
                .forEach { history.recordFinal(it.reminderKey()) }
            history.markBaselineDone()
        }

        val currentZone = zone()
        planner.plan(currentPackages, now, currentZone, history).forEach { reminder ->
            val delivered = bestEffort { sink.notify(reminder) } ?: false
            if (delivered) {
                bestEffort {
                    when (reminder.kind) {
                        ReminderKind.DAILY -> history.recordDaily(
                            ZonedDateTime.ofInstant(now, currentZone).toLocalDate()
                        )
                        ReminderKind.EXPIRED, ReminderKind.CONSUMED -> history.recordFinal(reminder.key)
                    }
                }
            }
        }

        bestEffort {
            history.prune(currentPackages.mapTo(HashSet<String>()) { it.reminderKey() })
        }
    }

    private suspend fun <T> bestEffort(block: suspend () -> T): T? =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
}
