package com.thegreatyamori.saldos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thegreatyamori.saldos.ui.SaldosRoot
import com.thegreatyamori.saldos.ui.SaldosViewModel
import com.thegreatyamori.saldos.ui.settings.SettingsScreen
import com.thegreatyamori.saldos.widget.BalanceWidgetReceiver
import com.thegreatyamori.saldos.widget.SquareBalanceWidgetReceiver
import com.thegreatyamori.saldos.widget.Widgets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as SaldosApp).container.repository
        val viewModelFactory = SaldosViewModel.factory(repository)

        setContent {
            com.thegreatyamori.saldos.ui.theme.SaldosTheme {
                val viewModel: SaldosViewModel = viewModel(factory = viewModelFactory)
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        state = state,
                        versionName = BuildConfig.VERSION_NAME,
                        onBack = { showSettings = false },
                        onLogout = {
                            viewModel.logout()
                            showSettings = false
                        },
                        onPolicyChange = viewModel::setRefreshPolicy,
                        onSelectLine = viewModel::selectLine,
                        onAddWidget = {
                            Widgets.requestPin(
                                this@MainActivity,
                                BalanceWidgetReceiver::class.java
                            )
                        },
                        onAddSquareWidget = {
                            Widgets.requestPin(
                                this@MainActivity,
                                SquareBalanceWidgetReceiver::class.java
                            )
                        }
                    )
                } else {
                    SaldosRoot(
                        state = state,
                        onLogin = viewModel::login,
                        onRefresh = viewModel::refresh,
                        onOpenSettings = { showSettings = true },
                        onSelectLine = viewModel::selectLine
                    )
                }
            }
        }
    }
}
