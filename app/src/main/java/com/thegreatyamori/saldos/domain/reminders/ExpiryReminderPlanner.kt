package com.thegreatyamori.saldos.domain.reminders

import com.thegreatyamori.saldos.domain.TrackedPackage
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ExpiryReminderPlanner(
    private val reminderDays: Long = DEFAULT_REMINDER_DAYS,
    private val quietStartHour: Int = DEFAULT_QUIET_START_HOUR,
    private val quietEndHour: Int = DEFAULT_QUIET_END_HOUR
) {

    fun plan(
        packages: List<TrackedPackage>,
        now: Instant,
        zone: ZoneId,
        history: ReminderHistory
    ): List<ExpiryReminder> {
        val unique = packages.distinctBy { it.reminderKey() }
        val finalEligible = unique.filter { it.hasFinalReminder(now) }
        val finalEligibleKeys = finalEligible.mapTo(HashSet()) { it.reminderKey() }
        val finals = finalEligible
            .filterNot { history.finalSent(it.reminderKey()) }
            .map { pkg ->
                val expiry = pkg.expiryInstant()
                ExpiryReminder(
                    key = pkg.reminderKey(),
                    kind = if (expiry != null && !now.isBefore(expiry)) {
                        ReminderKind.EXPIRED
                    } else {
                        ReminderKind.CONSUMED
                    },
                    expiresAt = expiry,
                    estimated = pkg.isEstimatedExpiry()
                )
            }

        val localNow = now.atZone(zone)
        val quiet = localNow.hour >= quietStartHour && localNow.hour < quietEndHour
        val active = unique
            .mapNotNull { pkg -> pkg.expiryInstant()?.let { pkg to it } }
            .filter { (pkg, expiry) ->
                pkg.isOpen() && now.isBefore(expiry) && pkg.reminderKey() !in finalEligibleKeys
            }
            .minByOrNull { (_, expiry) -> expiry }

        val daily = if (quiet && active != null) {
            val (pkg, expiry) = active
            val key = pkg.reminderKey()
            val today = localNow.toLocalDate()
            val daysLeft = ChronoUnit.DAYS
                .between(today, expiry.atZone(zone).toLocalDate())
                .toInt()
            val inWindow = daysLeft.toLong() in 0..reminderDays
            val alreadySent = history.lastDailyDate() == today
            if (inWindow && !alreadySent) {
                ExpiryReminder(
                    key = key,
                    kind = ReminderKind.DAILY,
                    expiresAt = expiry,
                    daysLeft = daysLeft,
                    estimated = pkg.isEstimatedExpiry()
                )
            } else {
                null
            }
        } else {
            null
        }

        return finals + listOfNotNull(daily)
    }

    private companion object {
        const val DEFAULT_REMINDER_DAYS = 3L
        const val DEFAULT_QUIET_START_HOUR = 8
        const val DEFAULT_QUIET_END_HOUR = 22
    }
}
