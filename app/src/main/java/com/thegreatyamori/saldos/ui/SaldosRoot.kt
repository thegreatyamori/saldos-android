package com.thegreatyamori.saldos.ui

import androidx.compose.runtime.Composable
import com.thegreatyamori.saldos.ui.home.HomeScreen
import com.thegreatyamori.saldos.ui.login.LoginScreen

@Composable
fun SaldosRoot(
    state: SaldosUiState,
    onLogin: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectLine: (String) -> Unit
) {
    val showLogin = !state.credentialsPresent && state.snapshot == null
    if (showLogin) {
        LoginScreen(
            refreshing = state.refreshing,
            error = state.error,
            onLogin = onLogin
        )
    } else {
        HomeScreen(
            state = state,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings,
            onSelectLine = onSelectLine
        )
    }
}
