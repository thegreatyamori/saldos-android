package com.thegreatyamori.saldos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thegreatyamori.saldos.SaldosApp
import com.thegreatyamori.saldos.data.cnt.CntException
import com.thegreatyamori.saldos.data.settings.allowsAutomaticRefresh
import com.thegreatyamori.saldos.domain.NoCredentialsException
import com.thegreatyamori.saldos.domain.RefreshResult
import com.thegreatyamori.saldos.util.Diag

class BalanceRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = (context.applicationContext as? SaldosApp)?.container?.repository
            ?: return Result.failure()

        val onWifi = NetworkPolicy.isOnWifi(context)
        if (!repository.refreshPolicy().allowsAutomaticRefresh(onWifi)) {
            Diag.step("worker skip policy=${repository.refreshPolicy()} wifi=$onWifi")
            return Result.success()
        }

        Diag.step("worker refresh policy=${repository.refreshPolicy()} wifi=$onWifi")
        return when (val result = repository.refresh()) {
            is RefreshResult.Success -> {
                Diag.step("worker success")
                Result.success()
            }
            is RefreshResult.Failure -> {
                Diag.warn("worker failure ${result.error.javaClass.simpleName}: ${result.error.message}")
                when (result.error) {
                    is NoCredentialsException, is CntException.InvalidCredentials -> Result.failure()
                    else -> Result.retry()
                }
            }
        }
    }
}
