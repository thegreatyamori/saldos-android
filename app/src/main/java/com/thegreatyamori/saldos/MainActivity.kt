package com.thegreatyamori.saldos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

                val context = LocalContext.current
                var hasSmsPermission by remember { mutableStateOf(hasReadSms(context)) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasSmsPermission = hasReadSms(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasSmsPermission = granted
                    if (granted) viewModel.setSmsTrackingEnabled(true)
                }
                val requestSms: () -> Unit = {
                    if (hasSmsPermission) {
                        viewModel.setSmsTrackingEnabled(true)
                    } else {
                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                }

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
                        },
                        onSmsTrackingChange = { enabled ->
                            if (enabled) requestSms() else viewModel.setSmsTrackingEnabled(false)
                        }
                    )
                } else {
                    SaldosRoot(
                        state = state,
                        onLogin = viewModel::login,
                        onRefresh = viewModel::refresh,
                        onOpenSettings = { showSettings = true },
                        onSelectLine = viewModel::selectLine,
                        onEnableSms = requestSms,
                        onDismissSms = viewModel::dismissSmsOptIn,
                        showSmsOptIn = !state.smsTrackingEnabled && !state.smsOptInDismissed
                    )
                }
            }
        }
    }
}

private fun hasReadSms(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED
