package com.thegreatyamori.saldos.data.notify

import android.content.Context
import androidx.core.content.edit
import com.thegreatyamori.saldos.domain.reminders.ReminderHistory
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class ExpiryReminderStore(
    context: Context,
    private val clock: () -> Instant = { Instant.now() }
) : ReminderHistory {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun lastDailyDate(): LocalDate? =
        prefs.getString(KEY_LAST_DAILY, null)?.let { stored ->
            runCatching { LocalDate.parse(stored) }.getOrNull()
        }

    override fun finalSent(key: String): Boolean = prefs.getBoolean(finalKey(key), false)

    override fun recordDaily(date: LocalDate) {
        prefs.edit { putString(KEY_LAST_DAILY, date.toString()) }
    }

    override fun recordFinal(key: String) {
        prefs.edit {
            putBoolean(finalKey(key), true)
            putLong(touchedKey(key), clock().toEpochMilli())
        }
    }

    override fun baselineDone(): Boolean = prefs.getBoolean(KEY_BASELINE, false)

    override fun markBaselineDone() {
        prefs.edit { putBoolean(KEY_BASELINE, true) }
    }

    override fun prune(activeKeys: Set<String>) {
        val activeHashes = activeKeys.mapTo(HashSet(), ::hash)
        val threshold = clock().toEpochMilli() - MAX_AGE_MS

        prefs.edit {
            prefs.all.keys.forEach { stored ->
                if (!stored.startsWith(FINAL_PREFIX)) return@forEach
                val hash = stored.removePrefix(FINAL_PREFIX)
                if (hash !in activeHashes && prefs.getLong(TOUCH_PREFIX + hash, 0L) < threshold) {
                    remove(FINAL_PREFIX + hash)
                    remove(TOUCH_PREFIX + hash)
                }
            }
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun finalKey(key: String): String = FINAL_PREFIX + hash(key)

    private fun touchedKey(key: String): String = TOUCH_PREFIX + hash(key)

    private fun hash(key: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                String.format(Locale.ROOT, "%02x", byte.toInt() and 0xff)
            }

    private companion object {
        const val PREFS = "saldos.expiry_reminders"
        const val KEY_BASELINE = "baseline_done"
        const val KEY_LAST_DAILY = "last_daily_date"
        const val FINAL_PREFIX = "final."
        const val TOUCH_PREFIX = "touched."
        const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }
}
