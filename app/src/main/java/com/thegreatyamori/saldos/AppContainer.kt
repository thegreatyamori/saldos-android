package com.thegreatyamori.saldos

import android.content.Context
import android.util.Log
import com.thegreatyamori.saldos.data.cache.SnapshotStore
import com.thegreatyamori.saldos.data.cnt.CntClient
import com.thegreatyamori.saldos.data.cnt.PersistentCookieJar
import com.thegreatyamori.saldos.data.creds.KeystoreCredentialStore
import com.thegreatyamori.saldos.data.notify.ExpiryReminderStore
import com.thegreatyamori.saldos.data.packages.ActivationStore
import com.thegreatyamori.saldos.data.packages.PackageUpdater
import com.thegreatyamori.saldos.data.settings.SettingsStore
import com.thegreatyamori.saldos.data.sms.CntSmsReader
import com.thegreatyamori.saldos.domain.BalancesRepository
import com.thegreatyamori.saldos.domain.DefaultBalancesRepository
import com.thegreatyamori.saldos.domain.SnapshotListener
import com.thegreatyamori.saldos.domain.reminders.ExpiryReminderCoordinator
import com.thegreatyamori.saldos.notify.AndroidReminderSink
import com.thegreatyamori.saldos.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext
    private val snapshotStore = SnapshotStore.create(applicationContext)
    private val settings = SettingsStore(applicationContext)
    private val reminderStore = ExpiryReminderStore(applicationContext)
    private val packageUpdater = PackageUpdater(
        reader = CntSmsReader(applicationContext.contentResolver),
        store = ActivationStore.create(applicationContext),
        settings = settings,
        onClear = {
            reminderStore.clear()
            AndroidReminderSink.cancelAll(applicationContext)
        }
    )

    val expiryReminderCoordinator = ExpiryReminderCoordinator(
        packages = packageUpdater::packages,
        enabled = { settings.expiryRemindersEnabled },
        history = reminderStore,
        sink = AndroidReminderSink(applicationContext)
    )

    private val client = CntClient(
        cookieJar = PersistentCookieJar(File(applicationContext.filesDir, "cookies.json"))
    )

    val repository: BalancesRepository = DefaultBalancesRepository(
        client = client,
        credentials = KeystoreCredentialStore(applicationContext),
        snapshots = snapshotStore,
        settings = settings,
        packages = packageUpdater,
        listener = SnapshotListener {
            withContext(Dispatchers.IO) {
                try {
                    Widgets.updateAll(applicationContext)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w("SaldosWidgets", "widget update failed", error)
                }
                try {
                    expiryReminderCoordinator.check()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w("SaldosReminders", "expiry check failed", error)
                }
            }
        }
    )
}
