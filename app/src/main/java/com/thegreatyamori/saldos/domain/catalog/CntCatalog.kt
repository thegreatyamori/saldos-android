package com.thegreatyamori.saldos.domain.catalog

import kotlin.math.abs

data class CntPackage(
    val priceUsd: Int,
    val dataMb: Int,
    val days: Int,
    val socialMb: Int = 0
)

object CntCatalog {

    const val VALID_AS_OF = "2026-10-27"

    private const val MB_PER_GB = 1024

    private val packages = listOf(
        CntPackage(priceUsd = 1, dataMb = 1 * MB_PER_GB, days = 1, socialMb = 512),
        CntPackage(priceUsd = 2, dataMb = 2 * MB_PER_GB, days = 3),
        CntPackage(priceUsd = 3, dataMb = 4 * MB_PER_GB, days = 7),
        CntPackage(priceUsd = 5, dataMb = 5 * MB_PER_GB, days = 30),
        CntPackage(priceUsd = 6, dataMb = 7 * MB_PER_GB, days = 30),
        CntPackage(priceUsd = 10, dataMb = 15 * MB_PER_GB, days = 30),
        CntPackage(priceUsd = 15, dataMb = 20 * MB_PER_GB, days = 30),
        CntPackage(priceUsd = 20, dataMb = 25 * MB_PER_GB, days = 30),
        CntPackage(priceUsd = 25, dataMb = 0, days = 30)
    )

    val all: List<CntPackage> = packages

    val byPrice: Map<Int, CntPackage> = packages.associateBy { it.priceUsd }

    val byDataMb: Map<Int, CntPackage> = packages
        .filter { it.dataMb > 0 }
        .associateBy { it.dataMb }

    fun forPrice(priceUsd: Int?): CntPackage? = priceUsd?.let { byPrice[it] }

    fun forDataMb(dataMb: Int): CntPackage? = byDataMb[dataMb]

    fun forDataMbApprox(dataMb: Int, tolerance: Double = 0.05): CntPackage? =
        byDataMb[dataMb] ?: packages.firstOrNull { candidate ->
            candidate.dataMb > 0 && abs(candidate.dataMb - dataMb) <= candidate.dataMb * tolerance
        }

    fun daysForDataMb(dataMb: Int): Int? = forDataMbApprox(dataMb)?.days
}
