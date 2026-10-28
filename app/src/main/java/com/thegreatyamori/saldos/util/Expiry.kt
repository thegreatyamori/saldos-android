package com.thegreatyamori.saldos.util

import com.thegreatyamori.saldos.domain.ExpiryMode
import com.thegreatyamori.saldos.domain.PackageSource
import com.thegreatyamori.saldos.domain.TrackedPackage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExpiryUi(val text: String, val estimated: Boolean)

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

/** The open package that expires first, which is the one worth showing. */
fun activePackage(packages: List<TrackedPackage>): TrackedPackage? =
    packages.filter { it.isOpen() }.minByOrNull { it.expiresAt }

fun expiryUi(pkg: TrackedPackage?, now: Instant = Instant.now()): ExpiryUi? {
    if (pkg == null) return null
    val expires = runCatching { Instant.parse(pkg.expiresAt) }.getOrNull() ?: return null
    val zone = ZoneId.systemDefault()
    val date = expires.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()

    val text = when {
        expires.isBefore(now) -> "venció el ${DATE_FORMAT.format(date)}"
        date == today -> "vence hoy"
        pkg.expiryMode == ExpiryMode.DAY_END -> "vence el ${DATE_FORMAT.format(date)} · 23:59"
        else -> "vence el ${DATE_FORMAT.format(date)}"
    }

    return ExpiryUi(
        text = text,
        estimated = pkg.source == PackageSource.DIFF || pkg.source == PackageSource.UNKNOWN
    )
}
