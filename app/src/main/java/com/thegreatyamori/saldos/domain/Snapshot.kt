package com.thegreatyamori.saldos.domain

import kotlinx.serialization.Serializable

@Serializable
data class BalanceItem(
    val label: String,
    val value: String,
    val unit: String,
    val total: String,
    val ratio: Float,
    val ring: Boolean,
    val color: String
)

@Serializable
data class LineInfo(
    val phone: String,
    val phoneValue: String,
    val plan: String
)

@Serializable
data class SnapshotHeader(
    val phone: String,
    val planTitle: String
)

@Serializable
data class SnapshotLine(
    val phone: String,
    val phoneValue: String
)

@Serializable
data class Snapshot(
    val serviceType: String,
    val header: SnapshotHeader,
    val line: SnapshotLine,
    val hero: BalanceItem?,
    val items: List<BalanceItem>,
    val lines: List<LineInfo>,
    val fetchedAt: String
)
