package com.thegreatyamori.saldos.domain

import com.thegreatyamori.saldos.data.cache.SnapshotStore
import com.thegreatyamori.saldos.data.cnt.CntClient
import com.thegreatyamori.saldos.data.creds.CredentialStore
import com.thegreatyamori.saldos.data.creds.Credentials
import com.thegreatyamori.saldos.data.settings.RefreshPolicy
import com.thegreatyamori.saldos.data.settings.SettingsStore

sealed interface RefreshResult {
    data class Success(val snapshot: Snapshot) : RefreshResult
    data class Failure(val error: Throwable, val cached: Snapshot?) : RefreshResult
}

class NoCredentialsException : IllegalStateException("No credentials stored")

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
    fun logout()
}

class DefaultBalancesRepository(
    private val client: CntClient,
    private val credentials: CredentialStore,
    private val snapshots: SnapshotStore,
    private val settings: SettingsStore
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

    override fun logout() {
        credentials.clear()
        snapshots.clear()
    }

    private suspend fun fetch(
        identifier: String,
        password: String,
        persistCredentials: Boolean
    ): RefreshResult = try {
        val snapshot = client.fetchBalances(
            identifier = identifier,
            password = password,
            preferredPhoneValue = preferredLine()
        )
        if (persistCredentials) {
            credentials.save(Credentials(identifier, password))
        }
        snapshots.write(snapshot)
        RefreshResult.Success(snapshot)
    } catch (error: Exception) {
        RefreshResult.Failure(error, cached())
    }
}
