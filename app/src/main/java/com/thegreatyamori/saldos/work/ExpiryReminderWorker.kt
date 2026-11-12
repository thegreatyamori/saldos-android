package com.thegreatyamori.saldos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thegreatyamori.saldos.SaldosApp
import kotlin.coroutines.cancellation.CancellationException

class ExpiryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as? SaldosApp)?.container
                ?: return Result.failure()
            container.expiryReminderCoordinator.check()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
