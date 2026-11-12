package com.thegreatyamori.saldos.domain.reminders

import java.time.LocalDate

interface ReminderHistory {
    fun lastDailyDate(): LocalDate?
    fun finalSent(key: String): Boolean
    fun recordDaily(date: LocalDate)
    fun recordFinal(key: String)
    fun baselineDone(): Boolean
    fun markBaselineDone()
    fun prune(activeKeys: Set<String>)
}
