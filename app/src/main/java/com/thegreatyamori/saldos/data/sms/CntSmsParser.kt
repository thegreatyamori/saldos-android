package com.thegreatyamori.saldos.data.sms

import com.thegreatyamori.saldos.domain.catalog.CntCatalog
import java.time.Instant
import java.util.Locale

enum class DaysSource { SMS, CATALOG_PRICE, CATALOG_DATA }

data class SmsActivation(
    val smsId: Long,
    val startedAt: Instant,
    val priceUsd: Int?,
    val dataMb: Int?,
    val days: Int,
    val daysSource: DaysSource,
    val unlimited: Boolean,
    val catalogMismatch: Boolean = false
)

class CntSmsParser {

    fun parse(sms: RawSms): SmsActivation? {
        val match = ACTIVATION.find(sms.body) ?: return null
        val price = match.groupValues[1].toWholeIntOrNull()
        val gbValue = match.groupValues[2].toDoubleOrNull()
        val unit = match.groupValues[3].uppercase(Locale.US)
        val textDays = match.groupValues[4].toIntOrNull()

        val unlimited = gbValue == null && sms.body.contains("ilimitad", ignoreCase = true)
        val dataMb = when {
            gbValue == null -> if (unlimited) 0 else null
            unit == "MB" -> gbValue.toInt()
            else -> (gbValue * 1024).toInt()
        }

        val days = textDays
            ?: CntCatalog.forPrice(price)?.days
            ?: dataMb?.let { CntCatalog.daysForDataMb(it) }
            ?: return null

        val daysSource = when {
            textDays != null -> DaysSource.SMS
            CntCatalog.forPrice(price) != null -> DaysSource.CATALOG_PRICE
            else -> DaysSource.CATALOG_DATA
        }

        val expectedDays = CntCatalog.forPrice(price)?.days ?: dataMb?.let { CntCatalog.daysForDataMb(it) }

        return SmsActivation(
            smsId = sms.id,
            startedAt = Instant.ofEpochMilli(sms.dateMs),
            priceUsd = price,
            dataMb = dataMb,
            days = days,
            daysSource = daysSource,
            unlimited = unlimited,
            catalogMismatch = expectedDays != null && expectedDays != days
        )
    }

    private fun String.toWholeIntOrNull(): Int? {
        val value = replace(',', '.').toDoubleOrNull() ?: return null
        return if (value % 1.0 == 0.0) value.toInt() else null
    }

    private companion object {
        val ACTIVATION = Regex(
            """CNT\s+informa.{0,200}?activado\s+tu\s+paquete.{0,200}?\${'$'}\s*(\d+(?:[.,]\d+)?)\s*(?:(\d+(?:[.,]\d+)?)\s*(GB|MB)\b)?(?:.{0,200}?vigencia\s+(?:de\s+)?(\d+)\s*d[iíIÍ]as?)?""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}
