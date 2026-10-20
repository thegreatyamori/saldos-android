package com.thegreatyamori.saldos.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val PERIODIC_WORK = "balance-refresh-periodic"
    private const val ONCE_WORK = "balance-refresh-once"

    private val connected = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<BalanceRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<BalanceRefreshWorker>()
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
