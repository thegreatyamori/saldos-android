package com.thegreatyamori.saldos

import android.app.Application
import com.thegreatyamori.saldos.work.WorkScheduler

class SaldosApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodic(this)
    }
}
