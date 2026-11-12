package com.thegreatyamori.saldos.data.packages

import com.thegreatyamori.saldos.data.sms.SmsActivation
import com.thegreatyamori.saldos.domain.BalanceItem
import com.thegreatyamori.saldos.domain.CloseReason
import com.thegreatyamori.saldos.domain.ExpiryMode
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.domain.TrackedPackage
import com.thegreatyamori.saldos.domain.catalog.CntCatalog
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Derives package activations from CNT SMS and, when there is no SMS, from snapshot diffs. */
class PackageTracker {

    fun reconcile(
        state: ActivationState,
        previous: Snapshot?,
        current: Snapshot,
        activations: List<SmsActivation>,
        now: Instant
    ): ActivationState {
        val packages = state.packages.toMutableList()

        val recent = activations.filter {
            it.startedAt.isAfter(now.minus(MAX_SMS_AGE_DAYS, ChronoUnit.DAYS))
        }
        addSmsActivations(packages, current, recent)
        addInferredActivations(packages, previous, current)
        markMatched(packages, current)
        closeFinished(packages, current, now)

        return state.copy(packages = packages)
    }

    private fun addSmsActivations(
        packages: MutableList<TrackedPackage>,
        current: Snapshot,
        activations: List<SmsActivation>
    ) {
        val known = packages.mapNotNull { it.smsId }.toSet()
        activations
            .filterNot { it.smsId in known }
            .forEach { sms ->
                val matching = quotaItems(current).firstOrNull { matches(sms.dataMb, sms.unlimited, it) }
                packages += TrackedPackage(
                    smsId = sms.smsId,
                    label = matching?.label,
                    priceUsd = sms.priceUsd,
                    dataMb = sms.dataMb,
                    days = sms.days,
                    startedAt = sms.startedAt.toString(),
                    expiresAt = expiryFor(sms.startedAt, sms.days, ExpiryMode.EXACT).toString(),
                    source = PackageSource.SMS,
                    expiryMode = ExpiryMode.EXACT
                )
            }
    }

    private fun addInferredActivations(
        packages: MutableList<TrackedPackage>,
        previous: Snapshot?,
        current: Snapshot
    ) {
        if (previous == null) return
        quotaItems(current).forEach { item ->
            val dataMb = totalMb(item) ?: return@forEach
            val alreadyTracked = packages.any { pkg ->
                pkg.isOpen() && (pkg.source == PackageSource.SMS || pkg.source == PackageSource.DIFF) &&
                    matches(pkg.dataMb, pkg.dataMb == 0, item)
            }
            if (alreadyTracked) return@forEach

            val days = CntCatalog.daysForDataMb(dataMb) ?: return@forEach
            if (!increased(previous, item, dataMb)) return@forEach

            val startedAt = runCatching { Instant.parse(previous.fetchedAt) }.getOrNull() ?: return@forEach
            packages += TrackedPackage(
                label = item.label,
                dataMb = dataMb,
                days = days,
                startedAt = startedAt.toString(),
                expiresAt = expiryFor(startedAt, days, ExpiryMode.EXACT).toString(),
                source = PackageSource.DIFF,
                expiryMode = ExpiryMode.EXACT
            )
        }
    }

    /**
     * A package is only closed for absence once it has been seen in a snapshot; an SMS may arrive
     * before CNT's API reflects the new quota.
     */
    private fun markMatched(packages: MutableList<TrackedPackage>, current: Snapshot) {
        for (index in packages.indices) {
            val pkg = packages[index]
            if (!pkg.isOpen() || pkg.matched) continue
            if (quotaItems(current).any { matches(pkg.dataMb, pkg.dataMb == 0, it) }) {
                packages[index] = pkg.copy(matched = true)
            }
        }
    }

    private fun closeFinished(
        packages: MutableList<TrackedPackage>,
        current: Snapshot,
        now: Instant
    ) {
        for (index in packages.indices) {
            val pkg = packages[index]
            if (!pkg.isOpen()) continue
            val expiresAt = runCatching { Instant.parse(pkg.expiresAt) }.getOrNull() ?: continue
            val matching = quotaItems(current).firstOrNull { matches(pkg.dataMb, pkg.dataMb == 0, it) }
            val uniqueLabel = pkg.label?.takeIf { label ->
                packages.count { it.isOpen() && it.label == label } == 1
            }
            val labelled = uniqueLabel?.let { label ->
                quotaItems(current).firstOrNull { it.label == label && isDataItem(it) }
            }
            val consumedItem = matching ?: labelled
            val stillPresent = matching != null && hasRemaining(matching)
            val consumed = consumedItem != null && !hasRemaining(consumedItem)
            val expired = !now.isBefore(expiresAt)
            val missing = !stillPresent && pkg.matched

            if (missing || expired) {
                val startedAt = runCatching { Instant.parse(pkg.startedAt) }.getOrNull()
                val calibrated = when {
                    startedAt == null -> pkg.expiryMode
                    missing && expired -> calibrate(startedAt, pkg.days, now)
                    else -> pkg.expiryMode
                }
                val reason = when {
                    expired -> CloseReason.EXPIRED
                    consumed -> CloseReason.CONSUMED
                    else -> CloseReason.MISSING
                }
                packages[index] = pkg.copy(
                    closed = true,
                    closeReason = reason,
                    expiryMode = calibrated,
                    expiresAt = startedAt?.let { expiryFor(it, pkg.days, calibrated).toString() } ?: pkg.expiresAt
                )
            }
        }
    }

    /** Compares when the quota actually ended against both candidate expiry models. */
    private fun calibrate(startedAt: Instant, days: Int, observedEnd: Instant): ExpiryMode {
        val exact = expiryFor(startedAt, days, ExpiryMode.EXACT)
        val dayEnd = expiryFor(startedAt, days, ExpiryMode.DAY_END)
        val exactDistance = abs(Duration.between(exact, observedEnd).seconds)
        val dayEndDistance = abs(Duration.between(dayEnd, observedEnd).seconds)
        return if (dayEndDistance < exactDistance) ExpiryMode.DAY_END else ExpiryMode.EXACT
    }

    private fun increased(previous: Snapshot, item: BalanceItem, dataMb: Int): Boolean {
        val before = quotaItems(previous).firstOrNull { it.label == item.label } ?: return true
        val beforeMb = totalMb(before) ?: return true
        return dataMb > beforeMb + MB_EPSILON
    }

    private fun quotaItems(snapshot: Snapshot): List<BalanceItem> =
        (listOfNotNull(snapshot.hero) + snapshot.items).distinctBy { it.label }

    private fun matches(dataMb: Int?, unlimited: Boolean, item: BalanceItem): Boolean {
        if (!isDataItem(item)) return false
        if (unlimited || dataMb == 0) return totalMb(item) == null
        if (dataMb == null) return false
        val mb = totalMb(item) ?: return false
        return abs(mb - dataMb) <= max(1, (dataMb * TOLERANCE).toInt())
    }

    private fun isDataItem(item: BalanceItem): Boolean =
        item.unit.uppercase() == "GB" || item.unit.uppercase() == "MB"

    /** CNT reports totals in the item unit ("5.00" GB), so everything is normalized to MB. */
    private fun totalMb(item: BalanceItem): Int? {
        if (!isDataItem(item)) return null
        val total = item.total.toDoubleOrNull() ?: return null
        if (total < 0) return null
        return when (item.unit.uppercase()) {
            "GB" -> (total * 1024).roundToInt()
            else -> total.roundToInt()
        }
    }

    private fun hasRemaining(item: BalanceItem): Boolean {
        if (item.total == "∞") return true
        val remaining = item.value.toDoubleOrNull() ?: return false
        return remaining > 0.0
    }

    private companion object {
        const val MB_EPSILON = 1
        const val TOLERANCE = 0.05
        const val MAX_SMS_AGE_DAYS = 45L
    }
}

fun expiryFor(startedAt: Instant, days: Int, mode: ExpiryMode): Instant {
    val start = if (mode == ExpiryMode.DAY_END) startOfDay(startedAt) else startedAt
    val end = start.plus(days.toLong(), ChronoUnit.DAYS)
    return if (mode == ExpiryMode.EXACT) end else endOfDay(end)
}

private fun startOfDay(instant: Instant): Instant =
    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()

private fun endOfDay(instant: Instant): Instant =
    startOfDay(instant).plus(1, ChronoUnit.DAYS).minusSeconds(1)
