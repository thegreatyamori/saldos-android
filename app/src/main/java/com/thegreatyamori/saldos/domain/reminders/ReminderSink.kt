package com.thegreatyamori.saldos.domain.reminders

interface ReminderSink {
    suspend fun notify(reminder: ExpiryReminder): Boolean
    suspend fun cancelAll()
}
