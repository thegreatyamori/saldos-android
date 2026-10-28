package com.thegreatyamori.saldos.domain

import kotlinx.serialization.Serializable

@Serializable
enum class PackageSource { SMS, DIFF, USER, UNKNOWN }

@Serializable
enum class ExpiryMode { EXACT, DAY_END }

@Serializable
data class TrackedPackage(
    val smsId: Long? = null,
    val label: String? = null,
    val priceUsd: Int? = null,
    val dataMb: Int? = null,
    val days: Int,
    val startedAt: String,
    val expiresAt: String,
    val source: PackageSource,
    val expiryMode: ExpiryMode = ExpiryMode.EXACT,
    val matched: Boolean = false,
    val closed: Boolean = false
) {
    fun isOpen(): Boolean = !closed
}
