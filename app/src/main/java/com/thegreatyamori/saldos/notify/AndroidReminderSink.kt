package com.thegreatyamori.saldos.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.thegreatyamori.saldos.MainActivity
import com.thegreatyamori.saldos.R
import com.thegreatyamori.saldos.domain.reminders.ExpiryReminder
import com.thegreatyamori.saldos.domain.reminders.ReminderKind
import com.thegreatyamori.saldos.domain.reminders.ReminderSink

class AndroidReminderSink(private val context: Context) : ReminderSink {

    private val notificationPrefs =
        context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)

    override suspend fun notify(reminder: ExpiryReminder): Boolean {
        if (!canNotify()) return false
        return try {
            val manager = NotificationManagerCompat.from(context)
            if (reminder.kind == ReminderKind.DAILY) {
                manager.cancel(dailyIdFor())
                notificationPrefs.edit { putString(KEY_LAST_DAILY, keyHash(reminder)) }
            } else if (notificationPrefs.getString(KEY_LAST_DAILY, null) == keyHash(reminder)) {
                manager.cancel(dailyIdFor())
            }
            manager.notify(idFor(reminder), notificationFor(reminder))
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override suspend fun cancelAll() {
        cancelExpiryNotifications(context)
    }

    private fun canNotify(): Boolean = canNotify(context)

    private fun notificationFor(reminder: ExpiryReminder): Notification {
        val id = idFor(reminder)
        val text = textFor(reminder)
        val contentIntent = pendingIntent(id)
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.expiry_notification_public))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .build()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.expiry_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun textFor(reminder: ExpiryReminder): String {
        val base = when (reminder.kind) {
            ReminderKind.DAILY -> when (reminder.daysLeft) {
                0 -> context.getString(R.string.expiry_notification_today)
                1 -> context.getString(R.string.expiry_notification_tomorrow)
                else -> context.resources.getQuantityString(
                    R.plurals.expiry_notification_days,
                    reminder.daysLeft ?: 0,
                    reminder.daysLeft ?: 0
                )
            }
            ReminderKind.EXPIRED -> context.getString(R.string.expiry_notification_expired)
            ReminderKind.CONSUMED -> context.getString(R.string.expiry_notification_consumed)
        }
        return if (reminder.estimated) {
            base + context.getString(R.string.expiry_notification_estimated_suffix)
        } else {
            base
        }
    }

    private fun idFor(reminder: ExpiryReminder): Int =
        if (reminder.kind == ReminderKind.DAILY) DAILY_NOTIFICATION_ID else finalIdFor(reminder)

    private fun dailyIdFor(): Int = DAILY_NOTIFICATION_ID

    private fun keyHash(reminder: ExpiryReminder): String = reminder.key.hashCode().toString()

    private fun finalIdFor(reminder: ExpiryReminder): Int =
        (reminder.key.hashCode() * 31 + 2) and Int.MAX_VALUE

    private fun pendingIntent(id: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "expiry_reminders"
        private const val DAILY_NOTIFICATION_ID = 1001
        private const val NOTIFICATION_PREFS = "saldos.expiry_notification"
        private const val KEY_LAST_DAILY = "last_daily_key"

        fun canNotify(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID)?.importance ==
                NotificationManager.IMPORTANCE_NONE
            ) {
                return false
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun cancelAll(context: Context) {
            cancelExpiryNotifications(context)
            context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
                .edit { remove(KEY_LAST_DAILY) }
        }

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.expiry_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.expiry_channel_description)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        private fun cancelExpiryNotifications(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.activeNotifications
                .filter { it.notification.channelId == CHANNEL_ID }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }
}
