package com.thegreatyamori.saldos.data.sms

import android.content.ContentResolver
import android.net.Uri
import java.time.Instant

data class RawSms(
    val id: Long,
    val address: String?,
    val body: String,
    val dateMs: Long
)

sealed interface SmsScan {
    data class Found(val messages: List<RawSms>) : SmsScan
    data object PermissionDenied : SmsScan
}

class CntSmsReader(private val resolver: ContentResolver) {

    fun read(since: Instant): SmsScan {
        val from = if (since.toEpochMilli() <= 0L) {
            Instant.now().minusSeconds(FIRST_SCAN_LOOKBACK_DAYS * 24 * 60 * 60).toEpochMilli()
        } else {
            since.toEpochMilli()
        }
        return try {
            val messages = mutableListOf<RawSms>()
            resolver.query(
                Uri.parse(INBOX),
                PROJECTION,
                "date > ?",
                arrayOf(from.toString()),
                "date ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("_id")
                val addressIndex = cursor.getColumnIndexOrThrow("address")
                val bodyIndex = cursor.getColumnIndexOrThrow("body")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                while (cursor.moveToNext()) {
                    messages += RawSms(
                        id = cursor.getLong(idIndex),
                        address = cursor.getString(addressIndex),
                        body = cursor.getString(bodyIndex).orEmpty(),
                        dateMs = cursor.getLong(dateIndex)
                    )
                }
            }
            SmsScan.Found(messages)
        } catch (_: SecurityException) {
            SmsScan.PermissionDenied
        }
    }

    private companion object {
        const val INBOX = "content://sms/inbox"
        const val FIRST_SCAN_LOOKBACK_DAYS = 90L
        val PROJECTION = arrayOf("_id", "address", "body", "date")
    }
}
