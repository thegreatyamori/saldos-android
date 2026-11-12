package com.thegreatyamori.saldos.domain.reminders

import com.thegreatyamori.saldos.domain.CloseReason
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.TrackedPackage
import java.time.Instant

enum class ReminderKind { DAILY, EXPIRED, CONSUMED }

data class ExpiryReminder(
    val key: String,
    val kind: ReminderKind,
    val expiresAt: Instant?,
    val daysLeft: Int? = null,
    val estimated: Boolean = false
)

fun TrackedPackage.reminderKey(): String =
    smsId?.let { "sms:$it" }
        ?: "pkg:${source.name}:${label.orEmpty()}:${dataMb ?: -1}:$startedAt"

fun TrackedPackage.expiryInstant(): Instant? =
    runCatching { Instant.parse(expiresAt) }.getOrNull()

fun TrackedPackage.hasFinalReminder(now: Instant): Boolean {
    val expired = expiryInstant()?.let { !now.isBefore(it) } ?: false
    return expired || closeReason == CloseReason.CONSUMED
}

fun TrackedPackage.isEstimatedExpiry(): Boolean =
    source == PackageSource.DIFF || source == PackageSource.UNKNOWN
