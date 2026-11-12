package com.thegreatyamori.saldos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thegreatyamori.saldos.ui.SaldosRoot
import com.thegreatyamori.saldos.ui.SaldosViewModel
import com.thegreatyamori.saldos.ui.settings.SettingsScreen
import com.thegreatyamori.saldos.notify.AndroidReminderSink
import com.thegreatyamori.saldos.widget.BalanceWidgetReceiver
import com.thegreatyamori.saldos.widget.SquareBalanceWidgetReceiver
import com.thegreatyamori.saldos.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SaldosApp).container
        val repository = container.repository
        val coordinator = container.expiryReminderCoordinator
        val viewModelFactory = SaldosViewModel.factory(repository)

        setContent {
            com.thegreatyamori.saldos.ui.theme.SaldosTheme {
                val viewModel: SaldosViewModel = viewModel(factory = viewModelFactory)
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                val context = LocalContext.current
                var hasSmsPermission by remember { mutableStateOf(hasReadSms(context)) }
                var hasNotificationPermission by remember {
                    mutableStateOf(hasNotificationsPermission(context))
                }
                var notificationsAvailable by remember {
                    mutableStateOf(AndroidReminderSink.canNotify(context))
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasSmsPermission = hasReadSms(context)
                            hasNotificationPermission = hasNotificationsPermission(context)
                            notificationsAvailable = AndroidReminderSink.canNotify(context)
                            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                coordinator.check()
                            }
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
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotificationPermission = granted
                    notificationsAvailable = AndroidReminderSink.canNotify(context)
                    viewModel.markNotificationsPermissionAsked()
                    if (granted) {
                        viewModel.setExpiryRemindersEnabled(true)
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            coordinator.check()
                        }
                    }
                }
                LaunchedEffect(
                    state.expiryRemindersEnabled,
                    hasNotificationPermission,
                    state.notificationsPermissionAsked
                ) {
                    if (state.expiryRemindersEnabled &&
                        !hasNotificationPermission &&
                        !state.notificationsPermissionAsked
                    ) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
                val requestSms: () -> Unit = {
                    if (hasSmsPermission) {
                        viewModel.setSmsTrackingEnabled(true)
                    } else {
                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                }
                val requestNotifications: () -> Unit = {
                    if (hasNotificationPermission) {
                        viewModel.setExpiryRemindersEnabled(true)
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            coordinator.check()
                        }
                    } else {
                        viewModel.setExpiryRemindersEnabled(true)
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
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
                        },
                        onExpiryRemindersChange = { enabled ->
                            if (enabled) {
                                requestNotifications()
                            } else {
                                viewModel.setExpiryRemindersEnabled(false)
                                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    coordinator.check()
                                }
                            }
                        },
                        notificationsAvailable = notificationsAvailable,
                        onOpenNotificationSettings = { openNotificationSettings(context) }
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

private fun hasNotificationsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}
