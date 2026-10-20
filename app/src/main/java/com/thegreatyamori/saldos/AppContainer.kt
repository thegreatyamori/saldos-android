package com.thegreatyamori.saldos

import android.content.Context
import com.thegreatyamori.saldos.data.cache.SnapshotStore
import com.thegreatyamori.saldos.data.cnt.CntClient
import com.thegreatyamori.saldos.data.cnt.PersistentCookieJar
import com.thegreatyamori.saldos.data.creds.KeystoreCredentialStore
import com.thegreatyamori.saldos.data.settings.SettingsStore
import com.thegreatyamori.saldos.domain.BalancesRepository
import com.thegreatyamori.saldos.domain.DefaultBalancesRepository
import java.io.File

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext
    private val snapshotStore = SnapshotStore.create(applicationContext)

    private val client = CntClient(
        cookieJar = PersistentCookieJar(File(applicationContext.filesDir, "cookies.json"))
    )

    val repository: BalancesRepository = DefaultBalancesRepository(
        client = client,
        credentials = KeystoreCredentialStore(applicationContext),
        snapshots = snapshotStore,
        settings = SettingsStore(applicationContext)
    )
}
