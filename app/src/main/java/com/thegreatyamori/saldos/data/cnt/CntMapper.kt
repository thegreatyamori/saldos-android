package com.thegreatyamori.saldos.data.cnt

import com.thegreatyamori.saldos.domain.BalanceItem
import com.thegreatyamori.saldos.domain.LineInfo
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.domain.SnapshotHeader
import com.thegreatyamori.saldos.domain.SnapshotLine
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Port of `saldos-api/src/application/mapper.ts`. */
object CntMapper {
    private val VOICE_METERS = setOf("Minutos")
    private val SKIP_TITLES = setOf("SEGUNDOS_OFFNET", "SEGUNDOS_ONNET", "SEGUNDOS_LDI")

    private val COLORS = mapOf(
        "GIGAS" to "#01A0E4",
        "FACEBOOK" to "#1877F2",
        "INSTAGRAM" to "#E1306C",
        "TIKTOK" to "#25F4EE",
        "WHATSAPP" to "#25D366",
        "SMS" to "#FFB020",
        "RECARGAS" to "#2ECC71"
    )
    private const val FALLBACK_COLOR = "#01A0E4"

    fun pickGroup(raw: List<RawGroup>, serviceType: String): RawGroup? =
        raw.firstOrNull { it.type == serviceType }
            ?: raw.firstOrNull { it.type == "movil" }
            ?: raw.firstOrNull()

    fun toLines(raw: List<RawGroup>, serviceType: String): List<LineInfo> =
        (pickGroup(raw, serviceType)?.data ?: emptyList()).map { line ->
            LineInfo(
                phone = line.phone.orEmpty(),
                phoneValue = line.phoneValue.orEmpty(),
                plan = line.title.orEmpty()
            )
        }

    fun toSnapshot(
        raw: List<RawGroup>,
        serviceType: String,
        preferredPhoneValue: String? = null
    ): Snapshot? {
        val lines = pickGroup(raw, serviceType)?.data ?: emptyList()
        val line = lines.firstOrNull { it.phoneValue == preferredPhoneValue } ?: lines.firstOrNull()
            ?: return null

        val phone = line.phone.orEmpty()
        val items = mapItems(line.info)
        val hero = items.firstOrNull { it.label == "GIGAS" } ?: items.firstOrNull()

        return Snapshot(
            serviceType = serviceType,
            header = SnapshotHeader(phone = headerPhone(phone), planTitle = line.title.orEmpty()),
            line = SnapshotLine(phone = headerPhone(phone), phoneValue = line.phoneValue.orEmpty()),
            hero = hero,
            items = items,
            lines = toLines(raw, serviceType),
            fetchedAt = java.time.Instant.now().toString()
        )
    }

    private fun headerPhone(phone: String): String =
        if (phone.length == 9) "0$phone" else phone

    private fun mapItems(info: List<RawBalanceItem>): List<BalanceItem> =
        info.filter { item ->
            item.data != null &&
                (item.data.meter ?: "") !in VOICE_METERS &&
                (item.title ?: "") !in SKIP_TITLES
        }.map { item ->
            val data = item.data!!
            val total = data.total.text().toDoubleOrNull() ?: 0.0
            // CNT reports the AVAILABLE/remaining quota in `consumed`.
            val remaining = data.consumed.text().toDoubleOrNull() ?: 0.0
            val unlimited = total.isFinite() && total < 0
            val meter = data.meter.orEmpty()
            val hasRingMeter = meter == "GB" || meter == "MB" || meter == "SMS"
            val usedShare = if (total > 0) (total - remaining) / total else 0.0
            val ring = hasRingMeter && !unlimited && total > 0

            BalanceItem(
                label = item.title.orEmpty(),
                value = if (unlimited) "∞" else formatNumber(remaining),
                unit = meter,
                total = if (unlimited) "∞" else data.total.text(),
                ratio = if (!ring || unlimited) 0f else min(1.0, max(0.0, usedShare)).toFloat(),
                ring = ring,
                color = COLORS[item.title] ?: FALLBACK_COLOR
            )
        }

    internal fun formatNumber(value: Double): String {
        if (!value.isFinite()) return value.toString()
        if (value == 0.0) return "0"
        val formatted = String.format(Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
        return formatted.ifEmpty { "0" }
    }

    private fun JsonElement?.text(): String = when (this) {
        null, is JsonNull -> ""
        is JsonPrimitive -> content
        else -> ""
    }
}
