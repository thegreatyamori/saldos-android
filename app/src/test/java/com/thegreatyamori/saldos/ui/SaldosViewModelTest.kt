package com.thegreatyamori.saldos.ui

import com.thegreatyamori.saldos.data.cnt.CntException
import com.thegreatyamori.saldos.data.settings.RefreshPolicy
import com.thegreatyamori.saldos.domain.BalanceItem
import com.thegreatyamori.saldos.domain.BalancesRepository
import com.thegreatyamori.saldos.domain.LineInfo
import com.thegreatyamori.saldos.domain.RefreshResult
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.domain.SnapshotHeader
import com.thegreatyamori.saldos.domain.SnapshotLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaldosViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful login exposes the snapshot and account`() = runTest(dispatcher) {
        val snapshot = sampleSnapshot()
        val repository = FakeBalancesRepository().apply {
            loginResult = RefreshResult.Success(snapshot)
        }
        val viewModel = SaldosViewModel(repository)

        viewModel.login("0983592938", "secret")
        advanceUntilIdle()

        assertEquals(snapshot, viewModel.state.value.snapshot)
        assertTrue(viewModel.state.value.credentialsPresent)
        assertEquals("0983592938", viewModel.state.value.accountIdentifier)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `blank fields fail fast`() = runTest(dispatcher) {
        val repository = FakeBalancesRepository()
        val viewModel = SaldosViewModel(repository)

        viewModel.login("   ", "")
        advanceUntilIdle()

        assertEquals(UiError.MissingFields, viewModel.state.value.error)
        assertNull(repository.lastLogin)
    }

    @Test
    fun `invalid credentials keep the user logged out`() = runTest(dispatcher) {
        val repository = FakeBalancesRepository().apply {
            loginResult = RefreshResult.Failure(CntException.InvalidCredentials(), null)
        }
        val viewModel = SaldosViewModel(repository)

        viewModel.login("0983592938", "wrong")
        advanceUntilIdle()

        assertEquals(UiError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.credentialsPresent)
    }

    @Test
    fun `refresh failure keeps cached data and maps the error`() = runTest(dispatcher) {
        val cached = sampleSnapshot()
        val repository = FakeBalancesRepository().apply {
            credentialsPresent = true
            snapshot = cached
            refreshResult = RefreshResult.Failure(CntException.Network(RuntimeException("offline")), cached)
        }
        val viewModel = SaldosViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(cached, viewModel.state.value.snapshot)
        assertEquals(UiError.Network, viewModel.state.value.error)
    }

    @Test
    fun `changing the policy persists and updates the state`() = runTest(dispatcher) {
        val repository = FakeBalancesRepository()
        val viewModel = SaldosViewModel(repository)

        viewModel.setRefreshPolicy(RefreshPolicy.ALWAYS)

        assertEquals(RefreshPolicy.ALWAYS, repository.policy)
        assertEquals(RefreshPolicy.ALWAYS, viewModel.state.value.refreshPolicy)
    }

    @Test
    fun `logout clears the account but keeps the policy`() = runTest(dispatcher) {
        val repository = FakeBalancesRepository().apply {
            credentialsPresent = true
            identifier = "0983592938"
            snapshot = sampleSnapshot()
            policy = RefreshPolicy.ALWAYS
        }
        val viewModel = SaldosViewModel(repository)

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.credentialsPresent)
        assertNull(viewModel.state.value.snapshot)
        assertEquals(RefreshPolicy.ALWAYS, viewModel.state.value.refreshPolicy)
    }

    private fun sampleSnapshot(): Snapshot = Snapshot(
        serviceType = "movil",
        header = SnapshotHeader(phone = "0983592938", planTitle = "PLAN PREPAGO CHIP PLUS"),
        line = SnapshotLine(phone = "0983592938", phoneValue = "servicio-0"),
        hero = BalanceItem(
            label = "GIGAS",
            value = "1.44",
            unit = "GB",
            total = "5.00",
            ratio = 0.712f,
            ring = true,
            color = "#01A0E4"
        ),
        items = emptyList(),
        lines = listOf(LineInfo(phone = "0983592938", phoneValue = "servicio-0", plan = "PLAN PREPAGO CHIP PLUS")),
        fetchedAt = "2026-10-19T20:00:00Z"
    )

    private class FakeBalancesRepository(
        var snapshot: Snapshot? = null,
        var credentialsPresent: Boolean = false,
        var identifier: String? = null,
        var policy: RefreshPolicy = RefreshPolicy.DATA_ONLY,
        var loginResult: RefreshResult? = null,
        var refreshResult: RefreshResult? = null
    ) : BalancesRepository {

        var lastLogin: Pair<String, String>? = null
        var selectedLine: String? = null

        override fun cached(): Snapshot? = snapshot

        override fun hasCredentials(): Boolean = credentialsPresent

        override suspend fun login(identifier: String, password: String): RefreshResult {
            lastLogin = identifier to password
            val result = loginResult ?: RefreshResult.Failure(
                IllegalStateException("login not configured"),
                snapshot
            )
            if (result is RefreshResult.Success) {
                credentialsPresent = true
                this.identifier = identifier
                snapshot = result.snapshot
            }
            return result
        }

        override suspend fun refresh(): RefreshResult =
            refreshResult ?: RefreshResult.Failure(
                IllegalStateException("refresh not configured"),
                snapshot
            )

        override fun selectLine(phoneValue: String) {
            selectedLine = phoneValue
        }

        override fun preferredLine(): String? = selectedLine

        override fun refreshPolicy(): RefreshPolicy = policy

        override fun setRefreshPolicy(policy: RefreshPolicy) {
            this.policy = policy
        }

        override fun identifier(): String? = identifier

        override suspend fun logout() {
            credentialsPresent = false
            identifier = null
            snapshot = null
        }
    }
}
