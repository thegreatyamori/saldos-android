package com.thegreatyamori.saldos.domain

import com.thegreatyamori.saldos.data.cache.SnapshotStore
import com.thegreatyamori.saldos.data.cnt.CntClient
import com.thegreatyamori.saldos.data.creds.CredentialStore
import com.thegreatyamori.saldos.data.creds.Credentials
import com.thegreatyamori.saldos.data.packages.PackageUpdater
import com.thegreatyamori.saldos.data.settings.RefreshPolicy
import com.thegreatyamori.saldos.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface RefreshResult {
    data class Success(val snapshot: Snapshot) : RefreshResult
    data class Failure(val error: Throwable, val cached: Snapshot?) : RefreshResult
}

class NoCredentialsException : IllegalStateException("No credentials stored")

fun interface SnapshotListener {
    suspend fun onSnapshotChanged()
}

interface BalancesRepository {
    fun cached(): Snapshot?
    fun hasCredentials(): Boolean
    suspend fun login(identifier: String, password: String): RefreshResult
    suspend fun refresh(): RefreshResult
    fun selectLine(phoneValue: String)
    fun preferredLine(): String?
    fun refreshPolicy(): RefreshPolicy
    fun setRefreshPolicy(policy: RefreshPolicy)
    fun identifier(): String?
    fun trackedPackages(): List<TrackedPackage>
    fun smsTrackingEnabled(): Boolean
    fun setSmsTrackingEnabled(enabled: Boolean)
    fun smsOptInDismissed(): Boolean
    fun dismissSmsOptIn()
    fun expiryRemindersEnabled(): Boolean
    fun setExpiryRemindersEnabled(enabled: Boolean)
    fun notificationsPermissionAsked(): Boolean
    fun markNotificationsPermissionAsked()
    suspend fun logout()
}

class DefaultBalancesRepository(
    private val client: CntClient,
    private val credentials: CredentialStore,
    private val snapshots: SnapshotStore,
    private val settings: SettingsStore,
    private val packages: PackageUpdater,
    private val listener: SnapshotListener
) : BalancesRepository {

    override fun cached(): Snapshot? = snapshots.read()

    override fun hasCredentials(): Boolean = credentials.load() != null

    override suspend fun login(identifier: String, password: String): RefreshResult =
        fetch(identifier, password, persistCredentials = true)

    override suspend fun refresh(): RefreshResult {
        val stored = credentials.load()
            ?: return RefreshResult.Failure(NoCredentialsException(), cached())
        return fetch(stored.identifier, stored.password, persistCredentials = false)
    }

    override fun selectLine(phoneValue: String) {
        settings.preferredPhoneValue = phoneValue
    }

    override fun preferredLine(): String? = settings.preferredPhoneValue

    override fun refreshPolicy(): RefreshPolicy = settings.refreshPolicy

    override fun setRefreshPolicy(policy: RefreshPolicy) {
        settings.refreshPolicy = policy
    }

    override fun identifier(): String? = credentials.load()?.identifier

    override fun trackedPackages(): List<TrackedPackage> = packages.packages()

    override fun smsTrackingEnabled(): Boolean = settings.smsTrackingEnabled

    override fun setSmsTrackingEnabled(enabled: Boolean) {
        settings.smsTrackingEnabled = enabled
    }

    override fun smsOptInDismissed(): Boolean = settings.smsOptInDismissed

    override fun dismissSmsOptIn() {
        settings.smsOptInDismissed = true
    }

    override fun expiryRemindersEnabled(): Boolean = settings.expiryRemindersEnabled

    override fun setExpiryRemindersEnabled(enabled: Boolean) {
        settings.expiryRemindersEnabled = enabled
    }

    override fun notificationsPermissionAsked(): Boolean = settings.notificationsPermissionAsked

    override fun markNotificationsPermissionAsked() {
        settings.notificationsPermissionAsked = true
    }

    override suspend fun logout() {
        credentials.clear()
        snapshots.clear()
        packages.clear()
        listener.onSnapshotChanged()
    }

    private suspend fun fetch(
        identifier: String,
        password: String,
        persistCredentials: Boolean
    ): RefreshResult = try {
        val previous = snapshots.read()
        val snapshot = client.fetchBalances(
            identifier = identifier,
            password = password,
            preferredPhoneValue = preferredLine()
        )
        if (persistCredentials) {
            credentials.save(Credentials(identifier, password))
        }
        snapshots.write(snapshot)
        runCatching { withContext(Dispatchers.IO) { packages.onSnapshot(previous, snapshot) } }
        listener.onSnapshotChanged()
        RefreshResult.Success(snapshot)
    } catch (error: Exception) {
        RefreshResult.Failure(error, cached())
    }
}
