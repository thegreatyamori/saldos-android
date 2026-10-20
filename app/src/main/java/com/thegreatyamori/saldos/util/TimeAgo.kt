package com.thegreatyamori.saldos.util

import java.time.Duration
import java.time.Instant

fun timeAgo(isoInstant: String): String {
    val instant = runCatching { Instant.parse(isoInstant) }.getOrNull() ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 60 * 24 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (60 * 24)} d"
    }
}

fun compactTimeAgo(isoInstant: String): String {
    val instant = runCatching { Instant.parse(isoInstant) }.getOrNull() ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        else -> "${minutes / (60 * 24)}d"
    }
}
